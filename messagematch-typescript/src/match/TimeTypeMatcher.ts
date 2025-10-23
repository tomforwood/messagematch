import {FieldMatcher} from "./fieldmatchers";
import {Temporal} from "@js-temporal/polyfill";
import Duration = Temporal.Duration;

type T = Temporal.PlainTime;

export default class TimeTypeMatcher extends FieldMatcher<T> {
    protected doMatch(value: string): boolean {
        return !!Temporal.PlainTime.from(value);
    }
    public asComparable(val: string):T {
        return Temporal.PlainTime.from(val);
    }
    public doSymRange(value: T, compareTo: T, s: string): boolean {
        return TimeTypeMatcher.doSymRangeTemporal(value, compareTo, s);
    }
    public doASymRange(value: T, compareTo: T, s: string): boolean {
        return TimeTypeMatcher.doASymRangeTemporal(value, compareTo, s);
    }

    protected static doSymRangeTemporal( value: T, compareTo: T, s: string) : boolean {
        const actual: Temporal.Duration  = value.until(compareTo).abs();
        const range: Temporal.Duration = TimeTypeMatcher.getDuration(s).abs();
        return Duration.compare(actual, range)<=0;
    }

    static getDuration(s: string): Duration {
        try{
            return Temporal.Duration.from(s);
        }
        catch (e) {
            const seconds = Number(s);
            return Duration.from({milliseconds: seconds});
        }
    }
    

    static doASymRangeTemporal(value: T,
                               compareTo: T,
                               s: string) : boolean {
        let start: T | Temporal.Instant;
        let end: T | Temporal.Instant;
        const range: Temporal.Duration = TimeTypeMatcher.getDuration(s);
        if (range.sign<0) {
            start = compareTo.add(range)
            end= compareTo;
        }
        else {
            start = compareTo;
            end = compareTo.add(range);
        }
        return Temporal.PlainTime.compare(value, start) >= 0 && Temporal.PlainTime.compare(value, end) <= 0;
    }

    compare(value: T, compareTo: T): 1 | 0 | -1 {
        return Temporal.PlainTime.compare(value, compareTo);
    }

}
