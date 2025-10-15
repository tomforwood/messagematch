import { readFileSync } from 'fs';
import MatcherLexer from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherLexer.js';
import MatcherParser from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser.js';
import GrammarListenerMatcher from './GrammarListenerMatcher';

// Lightweight stand-ins for the Java classes used in original
export type JsonNode = any;

export class JsonPath {
	constructor(public pathElement: string, public parent: JsonPath | null) {}
	toString() { return this.parent ? `${this.parent}:${this.pathElement}` : this.pathElement; }
}

export class MatchError {
	constructor(public path: JsonPath, public expected: string, public actual: string) {}
	toString() { return `Error at ${this.path} expected matching ${this.expected} but was ${this.actual}`; }
}

export interface FieldMatcher<T=any> {
	matches(actual: any, bindings: Map<string, any>): boolean;
}

export class JsonMatcher {
	static sizePattern = /([0-9]*)-([0-9]*)/;

	private errors: MatchError[] = [];
	private bindings: Map<string, any> = new Map();
	private matchTime = -1;

	constructor(private matcherNode: JsonNode, private concreteNode: JsonNode) {}

	static readNodes(json: string): JsonNode {
		return JSON.parse(json);
	}

	hasOption(_opt: any): boolean { return false; }

	matches(): boolean {
		if (this.matchTime < 0) this.matchTime = Date.now();
		const i = this.matchTime;
		const d = new Date(i);
		this.bindings.set('date', d.toISOString().slice(0,10));
		this.bindings.set('time', d.toISOString().slice(11,19));
		this.bindings.set('datetime', this.matchTime);
		return this.matchesNode(new JsonPath('root', null), this.matcherNode, this.concreteNode);
	}

	private matchesNode(path: JsonPath, matcherNode: JsonNode, concreteNode: JsonNode): boolean {
		if (matcherNode === null) {
			if (concreteNode !== null) {
				this.errors.push(new MatchError(path, 'a null value', String(concreteNode)));
				return false;
			}
			return true;
		}
		if (typeof matcherNode === 'string' || typeof matcherNode === 'number' || typeof matcherNode === 'boolean') {
			if (concreteNode === null || concreteNode === undefined) {
				this.errors.push(new MatchError(path, 'a value node', String(concreteNode)));
				return false;
			}
			return this.matchPrimitive(path, matcherNode, concreteNode);
		}
		if (Array.isArray(matcherNode)) {
			if (!Array.isArray(concreteNode)) {
				this.errors.push(new MatchError(path, 'an array node', String(concreteNode)));
				return false;
			}
			return this.matchArray(path, matcherNode, concreteNode);
		}
		if (typeof matcherNode === 'object') {
			if (typeof concreteNode !== 'object' || Array.isArray(concreteNode)) {
				this.errors.push(new MatchError(path, 'an object node', String(concreteNode)));
				return false;
			}
			return this.matchObject(path, matcherNode, concreteNode);
		}
		this.errors.push(new MatchError(path, `a match implementation for ${typeof matcherNode}`, 'Unimplemented'));
		return false;
	}

	private matchPrimitive(path: JsonPath, matcher: any, concrete: any): boolean {
		const matcherStr = String(matcher);
		const concreteStr = concrete == null ? null : String(concrete);

		let matches = false;
		if (matcherStr.startsWith('$')) {
			if (matcherStr.startsWith('$ID')) {
				// TODO: implement path extractor; for now treat as fail
				matches = false;
			} else {
				try {
					const fm = this.parseMatcher(matcherStr);
					matches = fm.matches(concrete, this.bindings);
				} catch (e:any) {
					this.errors.push(new MatchError(path, String(e.message || e), 'error'));
					return false;
				}
			}
		} else if (matcherStr.startsWith('\\$')) {
			const test = matcherStr.substring(1);
			matches = test === concreteStr;
		} else {
			matches = matcherStr === concreteStr;
		}

		if (!matches) this.errors.push(new MatchError(path, matcherStr, String(concrete)));
		return matches;
	}

	private parseMatcher(matcher: string): FieldMatcher {
		// Use antlr4 generated lexer/parser to build a matcher listener
		const antlr4 = require('antlr4');
		const chars = new antlr4.InputStream(matcher);
		const lexer = new MatcherLexer(chars);
		const tokens = new antlr4.CommonTokenStream(lexer);
		const parser = new MatcherParser(tokens);
		// throw on syntax errors
		parser.removeErrorListeners();
		parser.addErrorListener({
			syntaxError(_recognizer:any, _offendingSymbol:any, line:number, _charPos:number, msg:string) {
				throw new Error(`failed to parse at line ${line} due to ${msg}`);
			}
		});
		// Use the GrammarListenerMatcher to build a FieldMatcher
		const listener = new GrammarListenerMatcher();
		// parser.addParseListener is the ANTLR runtime method; generated TS parser has addParseListener
		if ((parser as any).addParseListener) {
			(parser as any).addParseListener(listener);
		}
		parser.matcher();
		if (!listener.result) throw new Error('cant parse matcher ' + matcher);
		return listener.result;
	}

	private matchArray(path: JsonPath, matcherNode: JsonNode[], concreteNode: JsonNode[]): boolean {
		// simplified: require same length and match items in order
		if (matcherNode.length !== concreteNode.length) {
			this.errors.push(new MatchError(path, `an array of size ${matcherNode.length}`, `size ${concreteNode.length}`));
			return false;
		}
		let ok = true;
		for (let i=0;i<matcherNode.length;i++) {
			ok = this.matchesNode(new JsonPath('['+i+']', path), matcherNode[i], concreteNode[i]) && ok;
		}
		return ok;
	}

	private matchObject(path: JsonPath, matcherNode: {[k:string]: any}, concreteNode: {[k:string]: any}): boolean {
		let result = true;
		const matchedKeys: string[] = [];
		let id: string|undefined;

		for (const key of Object.keys(matcherNode)) {
			const child = matcherNode[key];
			let matchedNodes: {[k:string]: any} = {};
			if (key.startsWith('$')) {
				if (key === '$Strict') { continue; }
				if (key === '$Size') {
					const bounds = String(child);
					const m = JsonMatcher.sizePattern.exec(bounds);
					if (!m) { this.errors.push(new MatchError(path, 'size should be "min-max"', bounds)); return false; }
					continue;
				}
				if (key === '$ID') { id = child; continue; }
				// matcher as key
				// TODO: parse matcher key and find matching concrete keys
				matchedNodes = {};
				for (const k of Object.keys(concreteNode)) {
					// naive: if key contains '*' treat as wildcard
					if (key.includes('*') || k === key) matchedNodes[k] = concreteNode[k];
				}
			} else if (key.startsWith('\\$')) {
				const realKey = key.substring(1);
				if (realKey in concreteNode) matchedNodes[realKey] = concreteNode[realKey];
			} else {
				if (key in concreteNode) matchedNodes[key] = concreteNode[key];
			}

			if (Object.keys(matchedNodes).length === 0) {
				this.errors.push(new MatchError(path, key, 'not present'));
				return false;
			} else {
				matchedKeys.push(key);
				for (const [k, v] of Object.entries(matchedNodes)) {
					result = this.matchesNode(new JsonPath(k, path), child, v) && result;
				}
			}
		}
        //TODO handle $Strict mode to check for unexpected keys

		if (result && id) this.bindings.set(id, concreteNode);
		return result;
	}

	getErrors(): MatchError[] { return this.errors; }
	getBindings(): Map<string, any> { return this.bindings; }
}

