import { NodeGenerator } from './NodeGenerator';

export class ReferenceGenerator extends NodeGenerator {
    constructor(private value: any) {
        super(undefined)
    }

    generate(): any {
        return this.value;
    }
}