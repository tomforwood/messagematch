import { NodeGenerator } from './NodeGenerator';

export class LiteralGenerator implements NodeGenerator {
    constructor(private value: any) {}

    generate(): any {
        return this.value;
    }
}