import { Temporal } from '@js-temporal/polyfill';

export interface Constraint {
    matches(o: any): boolean;
    generate(): any;
}

// Static constraints matching Java implementation
export const intConstraint: Constraint = {
    matches: (o: any) => typeof o === 'bigint' || (typeof o === 'number' && Number.isInteger(o)),
    generate: () => 0n // BigInt in TypeScript
};

export const numberConstraint: Constraint = {
    matches: (o: any) => typeof o === 'number' || typeof o === 'bigint',
    generate: () => 0.0
};

export const stringConstraint: Constraint = {
    matches: () => true,
    generate: () => "string"
};

export const boolConstraint: Constraint = {
    matches: (o: any) => typeof o === 'boolean' || o === 'true' || o === 'false',
    generate: () => true
};

export function instantConstraint(instant: bigint | number): Constraint {
    return {
        matches: (o: any) => typeof o === 'bigint' || (typeof o === 'number' && Number.isInteger(o)),
        generate: () => BigInt(instant)
    };
}

export function dateConstraint(localDate: string): Constraint {
    return {
        matches: (o: any) => {
            try {
                Temporal.PlainDate.from(o.toString());
                return true;
            } catch {
                return false;
            }
        },
        generate: () => localDate
    };
}

export function timeConstraint(localTime: string): Constraint {
    return {
        matches: (o: any) => {
            try {
                Temporal.PlainTime.from(o.toString());
                return true;
            } catch {
                return false;
            }
        },
        generate: () => localTime
    };
}

export class ValueProvider {
    private constraints: Set<Constraint> = new Set();
    private value: any = undefined;

    constructor(constraint?: Constraint) {
        if (constraint) {
            this.constraints.add(constraint);
        }
    }

    generate(): any {
        if (this.value !== undefined) return this.value;

        const possibles = new Set(Array.from(this.constraints).map(c => c.generate()));
        
        for (const possible of possibles) {
            if (Array.from(this.constraints).every(c => c.matches(possible))) {
                this.value = possible;
                return this.value;
            }
        }

        throw new Error("Unable to generate value that matches");
    }

    addConstraint(constraint: Constraint): void {
        this.constraints.add(constraint);
    }

    asInt(): number {
        const val = this.generate();
        return Number(val); // Constraint should ensure this is safe
    }

    asNum(): number {
        const val = this.generate();
        if (typeof val === 'bigint') {
            return Number(val);
        }
        return val as number;
    }

    asString(): string {
        return this.generate().toString();
    }

    asDate(): string {
        return this.generate().toString();
    }

    asInstant(): string {
        return Temporal.Instant.fromEpochMilliseconds(Number(this.asInt())).toString();
    }

    asTime(): string {
        return this.generate().toString();
    }

    asBoolean(): boolean {
        return Boolean(this.generate());
    }
}