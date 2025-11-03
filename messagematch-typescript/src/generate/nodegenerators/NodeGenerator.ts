import {
    boolConstraint,
    dateConstraint,
    instantConstraint,
    intConstraint,
    numberConstraint,
    stringConstraint, timeConstraint,
    ValueProvider
} from "../ValueProvider";

export abstract class NodeGenerator {
    protected constructor(protected valueProvider?: ValueProvider) {}
    abstract generate(): any;
}

export class StringTypeGenerator extends NodeGenerator {
    constructor(valueProvider: ValueProvider) {
        super(valueProvider);
        valueProvider.addConstraint(stringConstraint);
    }
    generate(): any {
        return this.valueProvider?.asString();
    }
}

export class IntTypeGenerator extends NodeGenerator{
    constructor(valueProvider: ValueProvider) {
        super(valueProvider);
        valueProvider.addConstraint(intConstraint)
    }
    generate(): any {
        return this.valueProvider?.asInt();
    }
}

export class NumTypeGenerator extends NodeGenerator{
    constructor(valueProvider: ValueProvider) {
        super(valueProvider);
        valueProvider.addConstraint(numberConstraint)
    }
    generate(): any {
        return this.valueProvider?.asNum();
    }
}

export class InstantTypeGenerator extends NodeGenerator{
    constructor(valueProvider: ValueProvider, instant:bigint|number) {
        super(valueProvider);
        valueProvider.addConstraint(instantConstraint(instant))
    }
    generate(): any {
        return this.valueProvider?.asInstant();
    }
}

export class DateTypeGenerator extends NodeGenerator{
    constructor(valueProvider: ValueProvider, date:string) {
        super(valueProvider);
        valueProvider.addConstraint(dateConstraint(date))
    }
    generate(): any {
        return this.valueProvider?.asDate();
    }
}

export class TimeTypeGenerator extends NodeGenerator{
    constructor(valueProvider: ValueProvider, time:string) {
        super(valueProvider);
        valueProvider.addConstraint(timeConstraint(time))
    }
    generate(): any {
        return this.valueProvider?.asTime();
    }
}

export class BoolTypeGenerator extends NodeGenerator{
    constructor(valueProvider: ValueProvider) {
        super(valueProvider);
        valueProvider.addConstraint(boolConstraint);
    }
    generate(): any {
        return this.valueProvider?.asBoolean();
    }
}