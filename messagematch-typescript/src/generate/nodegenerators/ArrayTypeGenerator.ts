import {NodeGenerator} from "./NodeGenerator";

export class ArrayTypeGenerator extends NodeGenerator{
    children:NodeGenerator[] = [];

    constructor() {
        super();
    }

    generate():any {
        const node:any[]  = [];
        this.children.forEach(child=>{
            node.push(child.generate());
        });
        return node;
    }

    addChild(child:NodeGenerator) {
        this.children.push(child);
    }

    setSize(min?:number, max?:number)
    {
        //if the size is less than specified duplicate teh last element until big enough
        //if it is greater - throw exception
        if (min && this.children.length < min)
        {
            const req = min - this.children.length;
            const toDupe = this.children[this.children.length - 1];
            for (let i = 0; i < req; i++) {
                this.addChild(toDupe);
            }
        }
        if (max && this.children.length > max)
        {
            throw new Error("max size of array is specified as "+ max + " but there are " + this.children.length + " elements specified")
        }
    }
}

/**
 * A sligtly hacky way to pass back the signal that the first entry in this array is a special
 * value that specifies the required size of the array rather than an actual node
 */
export class ArraySizeGenerator extends NodeGenerator{
    constructor(private _min?:number, private _max?:number) {
        super();
    }

    generate(): any {
    }

    get min(): number|undefined {
        return this._min;
    }

    get max(): number|undefined {
        return this._max;
    }
}