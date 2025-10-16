import { Interface } from "readline";

export abstract class FieldMatcher<T> {
  
  constructor(private binding: string | null, private nullable:boolean, private comparator: FieldComparator<T>){

  }

  
  matches(actual: string, bindings: Map<string, any>): boolean
  {
    if (this.binding!=null) {
			const existing = bindings.get(this.binding);
			if (existing!=null) {
				if (this.notEqual(actual, existing)) {
					return false;
				}
			}
			bindings.set(this.binding, actual);
		}
		if (actual==null) return this.nullable;
		let match = this.doMatch(actual);
		if (this.comparator!=null) {
			match = this.comparator.match(this.asComparable(value), bindings, this);
		}
		return match ;
  }

  abstract doMatch(value:string): boolean;
  abstract asComparable(val:string):T;

  private notEqual(value: string, existing: any): boolean {
		if (existing instanceof String) return !existing.equals(value);
		if (existing instanceof Date) {
			//the value can be represented a ms or ISO 8601
				const ms = Number(value);
        if (isNaN(ms))
        {
          return !(existing.toString() === value);
        }
				const epochMilli = existing.getTime();
				return ms != epochMilli;
			}
		return false;
	}
}

export class FieldComparator<T=any>{
  constructor(final op: string, final val: ValOrVar, eta: string){}
  

  
}

export class ValOrVar {
}

export class IntTypeMatcher extends FieldMatcher<bigint> {
  constructor(binding: string | null, nullable: boolean, comparator: FieldComparator<bigint>) {
    super(binding, nullable, comparator);
  }
  matches(actual: any) {
    if (actual==null) return this.nullable;
    return /^-?\d+$/.test(String(actual));
  }
}

export class NumTypeMatcher implements FieldMatcher {
  constructor(private binding: string | null, private nullable: boolean, private comparator: any) {}
  matches(actual: any) {
    if (actual==null) return this.nullable;
    return /^-?[0-9]+(\.[0-9]+)?([Ee][+-]?[0-9]+)?$/.test(String(actual));
  }
}

export class StringTypeMatcher implements FieldMatcher {
  constructor(private binding: string | null, private nullable: boolean, private comparator: any) {}
  matches(actual: any) {
    if (actual==null) return this.nullable;
    return typeof actual === 'string' || actual instanceof String;
  }
}

export class RegExpMatcher implements FieldMatcher {
  private re: RegExp;
  constructor(pattern: string, private binding: string | null, private nullable: boolean, private comparator: any) {
    this.re = new RegExp(pattern);
  }
  matches(actual: any) {
    if (actual==null) return this.nullable;
    return this.re.test(String(actual));
  }
}

export class BoolTypeMatcher implements FieldMatcher {
  constructor(private binding: string | null, private nullable: boolean, private comparator: any) {}
  matches(actual: any) {
    if (actual==null) return this.nullable;
    return actual === true || actual === false || String(actual) === 'true' || String(actual) === 'false';
  }
}
