

export class ValueProvider {

    private constraints:Set<Constraint> = new Set<Constraint>();

    constructor(private binding: string) {
        const constraint = new ProvidedConstraint(binding);
        this.constraints.add(constraint);
    }
}

class Constraint {}

class ProvidedConstraint extends Constraint {
    constructor(binding: string) {
        super();
    }

}