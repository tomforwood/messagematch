import {ValOrVarContext, ComparatorContext} from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser.js';

export abstract class FieldMatcher<T> {
  protected binding: string | null;
  protected nullable: boolean;
  protected comparator: FieldComparatorMatcher | null;

  constructor(binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher | null) {
    this.binding = binding;
    this.nullable = nullable;
    this.comparator = comparator;
  }

  matches(actual: string | null, bindings: Map<string, any>): boolean {
    if (this.binding != null) {
      const existing = bindings.get(this.binding);
      if (existing != null) {
        if (this.notEqual(actual, existing)) {
          return false;
        }
      }
      bindings.set(this.binding, actual);
    }
    if (actual == null) return this.nullable;
    let match = this.doMatch(actual);
    if (this.comparator != null) {
      match = this.comparator.match(this.asComparable(actual), bindings, this);
    }
    return match;
  }

  protected abstract doMatch(value: string): boolean;
  abstract asComparable(val: string): T;

  // range comparators used by FieldComparatorMatcher
  public abstract doSymRange(value: T, compareTo: T, s: string): boolean;
  public abstract doASymRange(value: T, compareTo: T, s: string): boolean;

  private notEqual(value: string | null, existing: any): boolean {
    if (existing == null) return value != null;
    if (typeof existing === 'string' || existing instanceof String) return String(existing) !== String(value);
    if (existing instanceof Date) {
      // the value can be represented as ms or ISO 8601
      if (value == null) return true;
      const ms = Number(value);
      if (isNaN(ms)) {
        return existing.toISOString() !== value;
      }
      const epochMilli = existing.getTime();
      return ms !== epochMilli;
    }
    return value?.toString() !== existing.toString();
  }
}

export class UnboundVariableException extends Error {
  varName: string;
  constructor(varName: string) {
    super(varName);
    this.varName = varName;
    this.name = 'UnboundVariableException';
  }
}

export class ValOrVar {
  public value: string | null;
  public variable: string | null;

  constructor(context: ValOrVarContext) {
    if (context.literal() != null) {
      this.value = context.literal().getText();
    } else {
      this.value = null;
    }
    if (context.variable() != null) {
      const v = context.variable();
        this.variable = v.IDENTIFIER().getText();
    } else {
      this.variable = null;
    }
  }
}

export class FieldComparator<T = any> {
  public op: string;
  public val: ValOrVar;
  public eta: string;
  // comp is expected to be a ComparatorContext from the generated parser
  constructor(comp: ComparatorContext) {
    this.op = comp._op.text;
    this.val = new ValOrVar(comp._val);
      this.eta = comp?._eta?.text;
  }

  protected vvToComp<U>(value: U, bindings: Map<string, any>, converter: (s: string) => U): U {
    let compareTo: U;
    if (this.val.variable != null) {
      const varVal = bindings.get(this.val.variable);
      if (varVal == null) throw new UnboundVariableException(this.val.variable);
      // if the variable value appears to be the same constructor/type as the provided value, use it directly
      try {
        if (varVal && value && (varVal.constructor === (value as any).constructor)) {
          compareTo = varVal as U;
        } else {
          compareTo = converter(String(varVal));
        }
      } catch (e) {
        compareTo = converter(String(varVal));
      }
    } else {
      // literal
      compareTo = converter(this.val.value as string);
    }
    return compareTo;
  }

  getOp(): string { return this.op; }
  getVal(): ValOrVar { return this.val; }
  getEta(): string | null { return this.eta; }

}

export class FieldComparatorMatcher extends FieldComparator {
  constructor(comp: ComparatorContext) {
    super(comp);
  }

  public match<T = any>(value: T, bindings: Map<string, any>, matcher: FieldMatcher<T>): boolean {
    const f = (s: string) => matcher.asComparable(s) as T;
    const compareTo = this.vvToComp<T>(value, bindings, f);
    return FieldComparatorMatcher.compare(value, matcher, compareTo, this.op, this.eta);
  }

  public static compare<T>(value: T, matcher: FieldMatcher<T>, compareTo: T, op: string, eta: string | null): boolean {
    switch (op) {
      case '++':
        return matcher.doASymRange(value, compareTo, eta || '0');
      case '+-':
        return matcher.doSymRange(value, compareTo, eta || '0');
      default:
        return FieldComparatorMatcher.basicComp(value, compareTo, op);
    }
  }

  public static basicComp<T>(val: T, compareVal: T, op: string): boolean {
    // assume val has compareTo semantics via JS comparison
    if (val == null || compareVal == null) return false;
    // For numbers and dates and BigInt we can compare using <,>,==
    switch (op) {
      case '>':
        return val > compareVal;
      case '<':
        return val < compareVal;
      case '>=':
        return val >= compareVal;
      case '<=':
        return val <= compareVal;
      default:
        return false;
    }
  }
}

export class IntTypeMatcher extends FieldMatcher<bigint> {
  static intPattern = /^-?\d+$/;
  constructor(binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher | null) {
    super(binding, nullable, comparator);
  }
  public asComparable(val: string): bigint {
    return BigInt(val);
  }

  protected doMatch(value: string): boolean {
    return IntTypeMatcher.intPattern.test(value);
  }

  public doSymRange(value: bigint, compareTo: bigint, s: string): boolean {
    const range = BigInt(s.startsWith('-') ? s.substring(1) : s);
    const diff = compareTo >= value ? compareTo - value : value - compareTo;
    return diff <= range;
  }

  public doASymRange(value: bigint, compareTo: bigint, s: string): boolean {
    const range = BigInt(s);
    let min: bigint;
    let max: bigint;
    if (range < BigInt(0)) {
      min = compareTo + range;
      max = compareTo;
    } else {
      min = compareTo;
      max = compareTo + range;
    }
    return value >= min && value <= max;
  }
}

export class NumTypeMatcher extends FieldMatcher<number> {
  static numPattern = /^-?[0-9]+(\.[0-9]+)?([Ee][+-]?[0-9]+)?$/;
  constructor(binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher | null) {
    super(binding, nullable, comparator);
  }
  public asComparable(val: string): number {
    return Number(val);
  }
  protected doMatch(value: string): boolean {
    return NumTypeMatcher.numPattern.test(value);
  }
  public doSymRange(value: number, compareTo: number, s: string): boolean {
    const range = Math.abs(Number(s));
    const diff = Math.abs(compareTo - value);
    return diff <= range;
  }
  public doASymRange(value: number, compareTo: number, s: string): boolean {
    const range = Number(s);
    let min: number;
    let max: number;
    if (range < 0) {
      min = compareTo + range;
      max = compareTo;
    } else {
      min = compareTo;
      max = compareTo + range;
    }
    return value >= min && value <= max;
  }
}

export class StringTypeMatcher extends FieldMatcher<string> {
  constructor(binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher | null) {
    super(binding, nullable, comparator);
  }
  public asComparable(val: string): string {
    return val;
  }
  protected doMatch(value: string): boolean {
    return true;
  }
  public doSymRange(_: string, __: string, ___: string): boolean {
    throw new Error("This doesn't really make sense");
  }
  public doASymRange(_: string, __: string, ___: string): boolean {
    throw new Error("This doesn't really make sense");
  }
}

export class RegExpMatcher extends FieldMatcher<string> {
  private re: RegExp;
  constructor(pattern: string, binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher| null) {
    super(binding, nullable, comparator);
    this.re = new RegExp(pattern);
  }
  public asComparable(val: string): string { return val; }
  protected doMatch(value: string): boolean { return this.re.test(String(value)); }
  public doSymRange(_: string, __: string, ___: string): boolean { throw new Error("Unsupported"); }
  public doASymRange(_: string, __: string, ___: string): boolean { throw new Error("Unsupported"); }
}

export class BoolTypeMatcher extends FieldMatcher<boolean> {
  constructor(binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher | null) {
    super(binding, nullable, comparator);
  }
  public asComparable(val: string): boolean { return val === 'true' || val === 'True' || val === '1'; }
  protected doMatch(value: string): boolean {
    return value === 'true' || value === 'false' || value === 'True' || value === 'False' || value === '1' || value === '0';
  }
  public doSymRange(_: boolean, __: boolean, ___: string): boolean { throw new Error('Unsupported'); }
  public doASymRange(_: boolean, __: boolean, ___: string): boolean { throw new Error('Unsupported'); }
}

export class TimeTypeMatcher extends FieldMatcher<number> {
  static numPattern = /^-?[0-9]+(\.[0-9]+)?([Ee][+-]?[0-9]+)?$/;
  constructor(binding: string | null, nullable: boolean, comparator: FieldComparatorMatcher) {
    super(binding, nullable, comparator);
  }
  public asComparable(val: string): number {
    return Number(val);
  }
  protected doMatch(value: string): boolean {
    return NumTypeMatcher.numPattern.test(value);
  }
  public doSymRange(value: number, compareTo: number, s: string): boolean {
    const range = Math.abs(Number(s));
    const diff = Math.abs(compareTo - value);
    return diff <= range;
  }
  public doASymRange(value: number, compareTo: number, s: string): boolean {
    const range = Number(s);
    let min: number;
    let max: number;
    if (range < 0) {
      min = compareTo + range;
      max = compareTo;
    } else {
      min = compareTo;
      max = compareTo + range;
    }
    return value >= min && value <= max;
  }
}
