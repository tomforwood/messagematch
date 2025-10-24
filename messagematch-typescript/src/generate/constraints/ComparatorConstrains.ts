import {Constraint, ValueProvider} from "../ValueProvider";
import {ComparatorContext} from "../../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser";
import {FieldComparator, FieldComparatorMatcher, IntTypeMatcher, NumTypeMatcher} from "../../match/fieldmatchers";
import {T} from "vitest/dist/chunks/global.d.MAmajcmJ";
import {ProvidedConstraint} from "./ProvidedConstraint";
import {computeIfAbsent} from "../GrammarListenerGenerator";

abstract class ComparatorBehaviour<T> {
    abstract doCast(o: any): T;
    abstract doMatches(val:T, compareTo:T,op: string, eta?:string): boolean;
    public abstract doGenerate( compareTo: T, op:string, eta?:string): T;
    generate(compareTo:any, op:string, eta?:string): T {
        return this.doGenerate(this.doCast(compareTo), op, eta);
    }
    public matches(val:any, compareTo:any, op:string, eta?:string):boolean {
        return this.doMatches(this.doCast(val), this.doCast(compareTo), op, eta);
    }
}


export class ComparatorConstraint implements Constraint{
    behavior: ComparatorBehaviour<any>;
    comparator:FieldComparator;
    constructor(comparator:ComparatorContext, protected bindings: Record<string, ValueProvider>, goalType:String){
        this.comparator = new FieldComparator(comparator);
        this.behavior = this.getBehaviour(goalType)
    }

    getBehaviour(goalType: String): ComparatorBehaviour<any> {
        switch (goalType) {
            case "$Instant" :
            case "$Int":
                return new IntegerBehaviour();
            case "$Num" :
                return new NumberBehaviour();
            case "$String" :

            case "$Date" :
            case "$Time" :
            default:
                throw new Error("Cant match against type "+goalType);
        }
    }

    generate(): any {
        const compareTo = this.toVP(this.comparator).generate();
        const eta = this.comparator.getEta();
        return this.behavior.generate(compareTo, this.comparator.getOp(), eta || undefined);
    }

    matches(generated: any): boolean {
        const compareTo = this.toVP(this.comparator).generate();
        const eta = this.comparator.getEta();
        return this.behavior.matches(generated, compareTo, this.comparator.getOp(), eta || undefined);
    }

    toVP(comparator:FieldComparator): ValueProvider {
        const vv = comparator.getVal();
        if (!!vv.value) {
            return new ValueProvider(new ProvidedConstraint(vv.value));
        } else if(!!vv.variable) {
            return computeIfAbsent(this.bindings, vv.variable, b=>new ValueProvider());
        }
        else throw new Error("Error with constraint"); //not possible with valid grammar (I think)
    }
}

class IntegerBehaviour extends ComparatorBehaviour<bigint>{
    doCast(o: any): bigint {
        return BigInt(o);
    }
    doMatches(val: bigint, compareTo: bigint, op: string, eta?: string): boolean {
        return FieldComparatorMatcher.compare(val, new IntTypeMatcher(null, false, null),compareTo,op, eta || null);
    }
    public doGenerate(compareTo: bigint, op: string, eta?: string): bigint {
        switch (op) {
            case ">=":
            case "<=":
            case "+-":
                return compareTo;
            case ">":
                return compareTo + BigInt(1);
            case "<":
                return compareTo - BigInt(1);
            case "++":
                return compareTo + BigInt(eta ||"0");
            default:
                return compareTo;
        }
    }
}

class NumberBehaviour extends ComparatorBehaviour<number>{
    doCast(o: any): number {
        return Number(o);
    }
    doMatches(val: number, compareTo: number, op: string, eta?: string): boolean {
        return FieldComparatorMatcher.compare(val, new NumTypeMatcher(null, false, null),compareTo,op, eta || null);
    }
    public doGenerate(compareTo: number, op: string, eta?: string): number {
        switch (op) {
            case ">=":
            case "<=":
            case "+-":
                return compareTo;
            case ">":
                return compareTo + Number(1);
            case "<":
                return compareTo - Number(1);
            case "++":
                return compareTo + Number(eta ||"0");
            default:
                return compareTo;
        }
    }

}