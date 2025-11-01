import {Temporal} from '@js-temporal/polyfill';
import {ValueProvider} from './ValueProvider';
import {ProvidedConstraint} from './constraints/ProvidedConstraint';
import {NodeGenerator} from './nodegenerators/NodeGenerator';
import {LiteralGenerator} from './nodegenerators/LiteralGenerator';
import {ReferenceGenerator} from './nodegenerators/ReferenceGenerator';
import {PathExtractor} from './utils/PathExtractor';
import {ObjectTypeGenerator} from "./nodegenerators/ObjectTypeGenerator";
import MatcherLexer from "../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherLexer";
import MatcherParser from "../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser";
import {GrammarListenerGenerator} from "./GrammarListenerGenerator";
import {ArraySizeGenerator, ArrayTypeGenerator} from "./nodegenerators/ArrayTypeGenerator";

export class JsonGenerator {
    // genTime can be set by tests to make generation deterministic
    genTime: number = -1;
    private matcherNode: any;
    private bindings: Record<string, ValueProvider>;
    private rawBindings: Record<string, any>;
    static readonly mapper = {}; // stub for now

    constructor(matcher: any, bindings: Record<string, any> = {}) {
        this.matcherNode = matcher;
        this.rawBindings = bindings;
        this.bindings = Object.fromEntries(
            Object.entries(bindings).map(([k, v]) => [k, this.createValueProvider(v.toString())])
        );
    }

    private parseMatcher(matcher: string): NodeGenerator {
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
        const grammarListenerGenerator = new GrammarListenerGenerator(this.bindings);
        parser.addParseListener(grammarListenerGenerator);
        parser.matcher();
        if (!grammarListenerGenerator.result) {
            throw new Error("Parsing of grammar failed");
        }
        return grammarListenerGenerator.result;
    }

    generate(): any {
        if (this.genTime < 0) {
            this.genTime = Temporal.Now.instant().epochMilliseconds;
        }

        const instant = Temporal.Instant.fromEpochMilliseconds(this.genTime);
        const zonedDateTime = instant.toZonedDateTimeISO('UTC');
        const date = zonedDateTime.toPlainDate();
        const time = zonedDateTime.toPlainTime();

        this.bindings['date'] = this.createValueProvider(date.toString());
        this.bindings['time'] = this.createValueProvider(time.toString());
        this.bindings['datetime'] = this.createValueProvider(this.genTime.toString());

        return this.generateNode(this.matcherNode).generate();
    }

    private generateNode(matcherNode: any): NodeGenerator {
        if (typeof matcherNode === 'string' || typeof matcherNode === 'boolean' || typeof matcherNode === 'number') {
            return this.generatePrimitive(matcherNode);
        }

        if (Array.isArray(matcherNode)) {
            return this.generateArray(matcherNode);
        }

        if (matcherNode === null) {
            return this.generateLiteral(null);
        }

        if (typeof matcherNode === 'object') {
            return this.generateObject(matcherNode);
        }

        return this.generateLiteral(matcherNode);
    }

    private generatePrimitive(matcherNode: any): NodeGenerator {
        const matcher = typeof matcherNode === 'object' ? matcherNode.asText?.() || matcherNode.toString() : matcherNode.toString();

        if (matcher.startsWith('$ID')) {
            const bound = PathExtractor.extractPrimitiveNode(matcher, this.rawBindings);
            return new ReferenceGenerator(bound);
        }

        if (matcher.startsWith('$')) {
            return this.parseMatcher(matcher);
        }

        if (matcher.startsWith('\\$')) {
            return new LiteralGenerator(matcher.replace(/^\\\$/, '$'));
        }

        return new LiteralGenerator(matcherNode);
    }

    private generateArray(arr: any[]): NodeGenerator {
        const arrayGen = new ArrayTypeGenerator();
        let sizeGen:ArraySizeGenerator | undefined
        arr.forEach(item => {
            const generate:NodeGenerator = this.generateNode(item);
            if (generate instanceof ArraySizeGenerator) {
               sizeGen = generate as ArraySizeGenerator;
            } else {
                arrayGen.addChild(generate);
            }
        });
        if (!!sizeGen) {
            arrayGen.setSize(sizeGen.min, sizeGen.max);
        }
        return arrayGen;
    }

    private sizePattern = /([0-9]*)-([0-9]*)/;
    private generateObject(matcherNode: any): NodeGenerator {
        const objectTypeGenerator:any = new ObjectTypeGenerator();
        let ownPropertyNames:string[] = Object.getOwnPropertyNames(matcherNode);
        for (let k of ownPropertyNames) {
            let value = matcherNode[k];
            if (k == "$Strict") continue;
            if (k == "$Size") {
                const bounds: string = value;
                const m = this.sizePattern.exec(bounds);
                if (m) {
                    const min = m[1] && m[1].length > 0 ? Number(m[1]) : undefined;
                    const max = m[2] && m[2].length > 0 ? Number(m[2]) : undefined;
                    return new ArraySizeGenerator(min, max);
                }
                return new ArraySizeGenerator();
            }
            if (k == "$ID") continue; //TODO implement this
            if (k.startsWith("$")) {
                const gen = this.parseMatcher(k);
                k = gen.generate();
            }
            objectTypeGenerator.addChild(k, this.generateNode(value));
        }
        return objectTypeGenerator;
    }

    private generateLiteral(value: any): NodeGenerator {
        return new LiteralGenerator(value);
    }

    private createValueProvider(val: string): ValueProvider {
        const res = new ValueProvider();
        res.addConstraint(new ProvidedConstraint(val));
        return res;
    }

    getBindings(): Record<string, string> {
        const result:Record<string, string> = {};
        Object.entries(this.bindings).forEach(([key, value]) => {
            result[key] = value.asString();
        });
        return result;
    }
}

export default JsonGenerator;
