import MatcherListener from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherListener.js';
import {
    FieldMatcher, IntTypeMatcher, NumTypeMatcher, StringTypeMatcher, RegExpMatcher, BoolTypeMatcher,
    FieldComparatorMatcher
} from './fieldmatchers';
import InstantTypeMatcher from './InstantTypeMatcher';
import DateTypeMatcher from './DateTypeMatcher';
import TimeTypeMatcher from './TimeTypeMatcher';
import {TypeMatcherContext} from "../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser";

// Extend the generated base listener so we inherit enterEveryRule/exitEveryRule etc.
export default class GrammarListenerMatcher extends MatcherListener {
    public result: FieldMatcher<any> | null = null;

    exitTypeMatcher = (ctx: TypeMatcherContext) => {
        // ANTLR TS generator stores tokens as Token objects on ctx (e.g. ctx._type_) which have a .text property
        const type =  ctx._type_.text;
        // binding() returns a BindingContext; its IDENTIFIER() is a TerminalNode with getText()
        const binding = ctx.binding()?.IDENTIFIER().getText();
        const nullable = !!ctx._nullable;
        // comparator handling
        let comparator = ctx._comp ? new FieldComparatorMatcher(ctx._comp) : null;

        switch (type) {
            case '$Int':
                this.result = new IntTypeMatcher(binding, nullable, comparator);
                break;
            case '$Num':
                this.result = new NumTypeMatcher(binding, nullable, comparator);
                break;
            case '$String':
                this.result = new StringTypeMatcher(binding, nullable, comparator);
                break;
            case '$Boolean':
                this.result = new BoolTypeMatcher(binding, nullable, comparator);
                break;
            case '$Instant':
                this.result = new InstantTypeMatcher(binding, nullable, comparator);
                break;
            case '$Date':
                this.result = new DateTypeMatcher(binding, nullable, comparator);
                break;
            case '$Time':
                this.result = new TimeTypeMatcher(binding, nullable, comparator);
                break;
            default:
                throw new Error('Cant match against type ' + type);
        }
    }

    exitRegexpMatcher = (ctx: any) => {
        const text = ctx.RE().getText();
        const binding = ctx.binding() ? ctx.binding().getText() : null;
        let regexp = text.substring(1, text.length-1);
        regexp = regexp.replace(/\\\^/g, '^');
        this.result = new RegExpMatcher(regexp, binding, false, null);
    }
}
