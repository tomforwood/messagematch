import { Constraint } from '../ValueProvider';

export class ProvidedConstraint implements Constraint {
    private readonly value: bigint | number | string;

    constructor(val: string) {
        let valStore: bigint | number | string;
        try {
            // Try BigInt first (equivalent to Java's BigInteger)
            valStore = BigInt(val);
        } catch {
            try {
                // Then try number (equivalent to Java's BigDecimal)
                const num = Number(val);
                if (isNaN(num)) throw new Error();
                valStore = num;
            } catch {
                // Fall back to string
                valStore = val;
            }
        }
        this.value = valStore;
    }

    matches(o: any): boolean {
        return o === this.value || o?.toString() === this.value?.toString();
    }

    generate(): any {
        return this.value;
    }
}