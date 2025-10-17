import { FieldMatcher } from './fieldmatchers';

// Port of Java InstantTypeMatcher. Uses Date objects for comparables.
export class InstantTypeMatcher extends FieldMatcher<Date> {
  // parses: string -> Date
  private static parses: (s: string) => Date = (s: string) => {
    // try ISO-8601 or other text parse
    const ms = Date.parse(s);
    if (!isNaN(ms)) return new Date(ms);
    // fallback to integer milliseconds
    const n = Number(s);
    if (!isNaN(n)) return new Date(n);
    throw new Error('cannot parse instant ' + s);
  };

  constructor(binding: string | null, nullable: boolean, comparator: any) {
    super(binding, nullable, comparator);
  }

  public asComparable(val: string): Date {
    try {
      return InstantTypeMatcher.parses(val);
    } catch (e) {
      // if parse throws, try numeric millis
      const ms = Number(val);
      return new Date(ms);
    }
  }

  protected doMatch(value: string): boolean {
    try {
      const d = InstantTypeMatcher.parses(String(value));
      return !isNaN(d.getTime());
    } catch (e) {
      return false;
    }
  }

  // parse a duration string into milliseconds.
  // Supports ISO-8601 durations starting with 'P'/'PT' (hours, minutes, seconds)
  // or a numeric seconds value (possibly fractional).
  private static getDurationMs(s: string): number {
    if (!s) return 0;
    // ISO-8601 duration basic support for PTnHnMnS
    if (s.startsWith('P') || s.startsWith('p')) {
      // e.g. PT1H2M3.5S
      const re = /P(?:([0-9]+)D)?(?:T(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+(?:\.[0-9]+)?)S)?)?/i;
      const m = re.exec(s);
      if (m) {
        const days = parseInt(m[1] || '0', 10);
        const hours = parseInt(m[2] || '0', 10);
        const mins = parseInt(m[3] || '0', 10);
        const secs = parseFloat(m[4] || '0');
        const ms = (((days * 24 + hours) * 60 + mins) * 60 + secs) * 1000;
        return ms;
      }
      // fallback: try parseFloat
    }
    // numeric seconds (possibly decimal)
    const asNum = Number(s);
    if (!isNaN(asNum)) {
      // Java code treated a plain number as seconds
      return asNum * 1000;
    }
    throw new Error('Invalid duration: ' + s);
  }

  // symmetric range: absolute difference <= range
  public static doSymRangeTemporal(value: Date, compareTo: Date, s: string): boolean {
    const actual = Math.abs(value.getTime() - compareTo.getTime());
    const range = Math.abs(InstantTypeMatcher.getDurationMs(s));
    return actual <= range;
  }

  // asymmetric: if range negative then start = compareTo + range, end = compareTo
  // else start = compareTo, end = compareTo + range
  public static doASymRangeTemporal(value: Date, compareTo: Date, s: string): boolean {
    const range = InstantTypeMatcher.getDurationMs(s);
    let startMs: number;
    let endMs: number;
    if (range < 0) {
      startMs = compareTo.getTime() + range;
      endMs = compareTo.getTime();
    } else {
      startMs = compareTo.getTime();
      endMs = compareTo.getTime() + range;
    }
    return value.getTime() >= startMs && value.getTime() <= endMs;
  }

  public doSymRange(value: Date, compareTo: Date, s: string): boolean {
    return InstantTypeMatcher.doSymRangeTemporal(value, compareTo, s);
  }

  public doASymRange(value: Date, compareTo: Date, s: string): boolean {
    return InstantTypeMatcher.doASymRangeTemporal(value, compareTo, s);
  }

  // public so it can be overridden
  public static setParses(fn: (s: string) => Date) {
    InstantTypeMatcher.parses = fn;
  }
}

export default InstantTypeMatcher;
