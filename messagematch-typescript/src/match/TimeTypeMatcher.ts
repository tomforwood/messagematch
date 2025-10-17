import InstantTypeMatcher from './InstantTypeMatcher';
import {FieldMatcher} from "./fieldmatchers";
import {Temporal} from "@js-temporal/polyfill";
import Duration = Temporal.Duration;


// TimeTypeMatcher will reuse InstantTypeMatcher parsing for now
export default class TimeTypeMatcher extends FieldMatcher<Temporal.PlainTime> {
    protected doMatch(value: string): boolean {
        return !!Temporal.PlainTime.from(value);
    }
    public asComparable(val: string):Temporal.PlainTime {
        return Temporal.PlainTime.from(val);
    }
    public doSymRange(value: Temporal.PlainTime, compareTo: Temporal.PlainTime, s: string): boolean {
        return TimeTypeMatcher.doSymRangeTemporal(value, compareTo, s);
    }
    public doASymRange(value: Temporal.PlainTime, compareTo: Temporal.PlainTime, s: string): boolean {
        return TimeTypeMatcher.doASymRangeTemporal(value, compareTo, s);
    }

    protected static doSymRangeTemporal( value: Temporal.PlainTime, compareTo: Temporal.PlainTime, s: string) : boolean {
        const actual: Temporal.Duration  = value.until(compareTo).abs();
        const range: Temporal.Duration = TimeTypeMatcher.getDuration(s).abs();
        return Duration.compare(actual, range)<=0;
    }

    protected static getDuration(s: string): Duration {
        let duration = Temporal.Duration.from(s);
        if (duration) return duration;
        const seconds = Number(s);
        duration = Duration.from({seconds: seconds});
        return duration;
    }

    protected static doASymRangeTemporal( value: Temporal.PlainTime, compareTo: Temporal.PlainTime, s: string) : boolean {
        let start: Temporal.PlainTime;
        let end: Temporal.PlainTime;
        const range: Temporal.Duration = TimeTypeMatcher.getDuration(s);
        if (range.sign<0) {
            start = compareTo.add(range)
            end= compareTo;
        }
        else {
            start = compareTo;
            end = compareTo.add(range);
        }
        return Temporal.PlainTime.compare(value, start)>=0 && Temporal.PlainTime.compare(value, end)<=0;
    }

    }
