export interface FieldMatcher<T=any> {
  matches(actual: any, bindings: Map<string, any>): boolean;
}

export class IntTypeMatcher implements FieldMatcher {
  constructor(private binding: string | null, private nullable: boolean, private comparator: any) {}
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
