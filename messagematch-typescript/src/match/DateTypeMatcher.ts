import {Temporal} from "@js-temporal/polyfill";
import {FieldMatcher} from "./fieldmatchers";
import TimeTypeMatcher from "./TimeTypeMatcher";

type T = Temporal.PlainDate;

export default class DateTypeMatcher extends FieldMatcher<T> {
    constructor(binding: string | null, nullable: boolean, comparator: any) {
        super(binding, nullable, comparator);
    }

    public asComparable(val: string): T {
        return Temporal.PlainDate.from(val);
    }

    protected doMatch(value: string): boolean {
        return !!Temporal.PlainDate.from(value);
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
        return Temporal.PlainDate.compare(value, start) >= 0 && Temporal.PlainDate.compare(value, end) <= 0;
    }

    compare(value: T, compareTo: T): 1 | 0 | -1 {
        return Temporal.PlainDate.compare(value, compareTo);
    }
}
