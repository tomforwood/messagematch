import { readFileSync } from 'fs';
import MatcherLexer from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherLexer.js';
import MatcherParser from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser.js';
import GrammarListenerMatcher from './GrammarListenerMatcher';
import {FieldMatcher, UnboundVariableException} from "./fieldmatchers";

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

export class JsonMatcher {
	static sizePattern = /([0-9]*)-([0-9]*)/;

	private errors: MatchError[] = [];
	private bindings: Map<string, any> = new Map();
	private options = new Set<string>();
	matchTime = -1;

	constructor(private matcherNode: JsonNode, private concreteNode: JsonNode) {}

	static readNodes(json: string): JsonNode {
		return JSON.parse(json);
	}

	hasOption(opt: string): boolean {
		return this.options.has(opt);
	}

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
					if (e instanceof UnboundVariableException) {}
					this.errors.push(new MatchError(path, (e as UnboundVariableException).varName + " to be bound", 'unbound'));
					return false;
				}
			}
		} else if (matcherStr.startsWith('\\$')) {
			const test = matcherStr.substring(1);
			matches = test === concreteStr;
		} else {
			matches = matcherStr === concreteStr;
		}

		if (!matches)
			this.errors.push(new MatchError(path, matcherStr, String(concrete)));
		return matches;
	}

	private parseMatcher(matcher: string): FieldMatcher<any> {
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

		parser.addParseListener(listener);

		parser.matcher();
		if (!listener.result) throw new Error('cant parse matcher ' + matcher);
		return listener.result;
	}

	private matchArray(path: JsonPath, matcherNode: JsonNode[], concreteNode: JsonNode[]): boolean {
		let matcherSize = matcherNode.length;
		const concreteSize = concreteNode.length;

		// special flags
		let strict = this.hasOption('$Strict');
		let unordered = this.hasOption('$Unordered');
		let each = false;
		let min = 0;
		let max = Number.MAX_SAFE_INTEGER;
		let offset = 0;
		let hasSize = false;

		if (matcherSize === 0) {
			if (concreteSize === 0) return true;
			this.errors.push(new MatchError(path, "an empty array", `size ${concreteSize}`));
			return false;
		}

		// look for special matching node
		const n = matcherNode[0];
		if (typeof n === 'object' && n !== null && !Array.isArray(n)) {
			const keys = Object.keys(n);
			if (keys.length > 0 && keys[0].startsWith('$')) {
				// this is a "special" object
				strict = '$Strict' in n;
				unordered = '$Unordered' in n;
				each = '$Each' in n;
				const sizeNode = n['$Size'];
				if (sizeNode !== undefined) {
					hasSize = true;
					const bounds = String(sizeNode);
					const m = JsonMatcher.sizePattern.exec(bounds);
					if (m) {
						if (m[1].length > 0) {
							min = parseInt(m[1], 10);
						}
						if (m[2].length > 0) {
							max = parseInt(m[2], 10);
						}
					} else {
						this.errors.push(new MatchError(path, "size should be \"min-max\"", bounds));
						return false;
					}
				}
				if (strict || unordered || each || hasSize) {
					offset = 1;
					matcherSize--;
				}
			}
		}

		let matches = true;

		if (concreteSize < matcherSize) {
			this.errors.push(new MatchError(path, `an array of at least size ${matcherSize}`, String(concreteSize)));
			return false;
		}

		if (strict && concreteSize > matcherSize) {
			this.errors.push(new MatchError(path, `an array of exactly size ${matcherSize}`, String(concreteSize)));
			matches = false;
		}

		if (concreteSize < min) {
			this.errors.push(new MatchError(path, `an array of at least size ${min}`, String(concreteSize)));
			matches = false;
		}
		if (concreteSize > max) {
			this.errors.push(new MatchError(path, `an array of at most size ${max}`, String(concreteSize)));
			matches = false;
		}

		if (!unordered) {
			for (let i = 0; i < matcherSize; i++) {
				const matcherChild = each ? matcherNode[offset] : matcherNode[i + offset];
				const concreteChild = concreteNode[i];
				matches = this.matchesNode(new JsonPath(`[${i}]`, path), matcherChild, concreteChild) && matches;
			}
		} else {
			const leftToMatch = [...concreteNode];
			// The test matchings here will mess up our errors list so temporarily replace it
			const realErrors = this.errors;
			this.errors = [];
			for (let i = 0; i < matcherSize; i++) {
				const matcherChild = each ? matcherNode[offset] : matcherNode[i + offset];
				let isMatched = false;
				for (let j = 0; j < leftToMatch.length; j++) {
					isMatched = this.matchesNode(new JsonPath(`[${i}]`, path), matcherChild, leftToMatch[j]);
					if (isMatched) {
						leftToMatch.splice(j, 1);
						break;
					}
				}
				if (!isMatched) {
					realErrors.push(new MatchError(path, `an object matching ${JSON.stringify(matcherChild)}`, "nothing matching"));
				}
				matches = isMatched && matches;
			}
			this.errors = realErrors;
		}
		return matches;
	}

	private matchObject(path: JsonPath, matcherNode: {[k:string]: any}, concreteNode: {[k:string]: any}): boolean {
		let result = true;
		const matchedKeys: string[] = [];
		let strictMode = false;
		let id: string|undefined;

		for (const key of Object.keys(matcherNode)) {
			const child = matcherNode[key];
			let matchedNodes: {[k:string]: any} = {};
			if (key.startsWith('$')) {
				if (key === '$Strict') {
					strictMode = true;
					continue;
				}
				if (key === '$Size') {
					const bounds = String(child);
					const m = JsonMatcher.sizePattern.exec(bounds);
					if (!m) { this.errors.push(new MatchError(path, 'size should be "min-max"', bounds)); return false; }
					throw new Error("Not implemented this");
					continue;
				}
				if (key === '$ID') { id = child; continue; }
				// matcher as key
				const matcher: FieldMatcher<any> = this.parseMatcher(key);
				matchedNodes = {};
				for (const k of Object.keys(concreteNode)) {
					if (matcher.matches(k, this.bindings)) {
						matchedNodes[k] = concreteNode[k];
					}
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

		if (strictMode) {
			let concreteKeys:string[] = []; 
			for (const key of Object.keys(concreteNode)) {
				concreteKeys.push(key);
			}
			
			concreteKeys = concreteKeys.filter(k=>!matchedKeys.includes(k));
			if (concreteKeys.length>0) {
				this.errors.push(new MatchError(path, 'no additional values', '['+concreteKeys.join(', ')+']'));
				result = false;
			}
		}

		if (result && id) this.bindings.set(id, concreteNode);
		return result;
	}

	getErrors(): MatchError[] { return this.errors; }
	getBindings(): Map<string, any> { return this.bindings; }
}
