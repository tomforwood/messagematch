import { Temporal } from '@js-temporal/polyfill';
import { ValueProvider } from './ValueProvider';
import { ProvidedConstraint } from './constraints/ProvidedConstraint';
import { NodeGenerator } from './nodegenerators/NodeGenerator';
import { LiteralGenerator } from './nodegenerators/LiteralGenerator';
import { ReferenceGenerator } from './nodegenerators/ReferenceGenerator';
import { PathExtractor } from './utils/PathExtractor';

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
        // Stub implementation - to be completed later
        return new LiteralGenerator(matcher);
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

    // Stub implementations of helper methods - to be implemented later
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
        return {
            generate: () => []
        };
    }

    private generateObject(obj: Record<string, any>): NodeGenerator {
        return {
            generate: () => ({})
        };
    }

    private generateLiteral(value: any): NodeGenerator {
        return {
            generate: () => value
        };
    }

    private createValueProvider(val: string): ValueProvider {
        const res = new ValueProvider();
        res.addConstraint(new ProvidedConstraint(val));
        return res;
    }
}

export default JsonGenerator;
