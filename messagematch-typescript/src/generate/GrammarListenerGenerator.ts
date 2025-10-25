import {ValueProvider} from "./ValueProvider";
import {
    BoolTypeGenerator,
    DateTypeGenerator,
    InstantTypeGenerator,
    IntTypeGenerator,
    NodeGenerator,
    NumTypeGenerator,
    StringTypeGenerator, TimeTypeGenerator
} from "./nodegenerators/NodeGenerator";
import MatcherListener from "../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherListener";
import {
    RegexpMatcherContext,
    TypeMatcherContext
} from "../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser";
import {ProvidedConstraint} from "./constraints/ProvidedConstraint";
import {ComparatorConstraint} from "./constraints/ComparatorConstrains";

export function  computeIfAbsent<T>(bindings: Record<string, T>, binding: string, ifAbsent: (b:string) => T): T {
    let existing = bindings[binding];
    if (existing) return existing;
    existing = ifAbsent(binding);
    bindings[binding] = existing;
    return existing;
}

export class GrammarListenerGenerator extends MatcherListener{
    result?:NodeGenerator;
    private bindings: Record<string, ValueProvider>;
    constructor(bindings: Record<string, ValueProvider>) {
        super();
        this.bindings = bindings;
    }

    exitTypeMatcher = (ctx: TypeMatcherContext) => {
        // ANTLR TS generator stores tokens as Token objects on ctx (e.g. ctx._type_) which have a .text property
        const type = ctx._type_.text;
        const defaultVal = ctx.genValue();
        let provider:ValueProvider;
        if (!ctx.binding())
        {
            provider = new ValueProvider();
        }
        else {
            const binding = ctx.binding().IDENTIFIER().getText();
            provider = computeIfAbsent(this.bindings, binding, (b)=> new ValueProvider());
        }
        if (defaultVal) {
            const genVal:string = defaultVal.getText().substring(1);
            provider.addConstraint(new ProvidedConstraint(genVal));
        }
        if (ctx.comparator())
        {
            provider.addConstraint(new ComparatorConstraint(ctx.comparator(), this.bindings, type));
        }
        switch (ctx._type_.text){
            case "$Int":
                this.result = new IntTypeGenerator(provider);
                break;
            case "$String":
                this.result = new StringTypeGenerator(provider);
                break;
            case "$Num":
                this.result = new NumTypeGenerator(provider);
                break;
            case "$Instant":
                this.result = new InstantTypeGenerator(provider, this.bindings["datetime"].asInt());
                break;
            case "$Date" :
                this.result = new DateTypeGenerator(provider, this.bindings["date"].asString());
                break;
            case "$Time" :
                this.result = new TimeTypeGenerator(provider, this.bindings["time"].asString());
                break;
            case "$Boolean" :
                this.result = new BoolTypeGenerator(provider);
                break;
            default:
                throw new Error("Unsupported type in jsonGenerator of "+ctx._type_.text);
        }

    }


    exitRegexpMatcher = (ctx: RegexpMatcherContext) => {
        let provider: ValueProvider;
        if (!ctx.binding()) {
            provider = new ValueProvider();
        } else {
            let binding = ctx.binding().IDENTIFIER().getText();
            provider = computeIfAbsent(this.bindings, binding, (b)=> new ValueProvider());
        }
        if (!!ctx.genValue()) {
            let genValue = ctx.genValue().getText().substring(1);
            provider.addConstraint(new ProvidedConstraint(genValue));
        }
        this.result = new StringTypeGenerator(provider);
    };
}