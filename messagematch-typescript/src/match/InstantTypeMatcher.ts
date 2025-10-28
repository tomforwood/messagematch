import {FieldMatcher} from './fieldmatchers';
import {Temporal} from "@js-temporal/polyfill";
import TimeTypeMatcher from "./TimeTypeMatcher";

type T = Temporal.Instant;
// Port of Java InstantTypeMatcher. Uses Date objects for comparables.
export class InstantTypeMatcher extends FieldMatcher<T> {
  // parses: string -> Date

  constructor(binding: string | null, nullable: boolean, comparator: any) {
    super(binding, nullable, comparator);
  }

  public asComparable(val: string): T {
    try {
      return Temporal.Instant.from(val);
    }
    catch (e) {
      // if parse has failed try intepreting as epoch millis
      const ms = Number(val);
      return Temporal.Instant.fromEpochMilliseconds(ms);
    }
  }

  protected doMatch(value: string): boolean {
    try {
      return !!this.asComparable(value);
    }
    catch (e) {
      return false;
    }
  }

  // symmetric range: absolute difference <= range
  doSymRange( value: T, compareTo: T, s: string) : boolean {
    const actual: Temporal.Duration  = value.until(compareTo).abs();
    const range: Temporal.Duration = TimeTypeMatcher.getDuration(s).abs();
    return Temporal.Duration.compare(actual, range)<=0;
  }


  doASymRange(value: T,
                             compareTo: T,
                             s: string) : boolean {
    let start: T;
    let end: T;
    const range: Temporal.Duration = TimeTypeMatcher.getDuration(s);
    if (range.sign<0) {
      start = compareTo.add(range)
      end= compareTo;
    }
    else {
      start = compareTo;
      end = compareTo.add(range);
    }
    return Temporal.Instant.compare(value, start) >= 0 && Temporal.Instant.compare(value, end) <= 0;
  }

  compare(value: T, compareTo: T): 1 | 0 | -1 {
    return Temporal.Instant.compare(value, compareTo);
  }
}

export default InstantTypeMatcher;
