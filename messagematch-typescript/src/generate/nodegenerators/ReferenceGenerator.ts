import { NodeGenerator } from './NodeGenerator';

export class ReferenceGenerator implements NodeGenerator {
    constructor(private value: any) {}

    generate(): any {
        return this.value;
    }
}