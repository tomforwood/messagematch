import {NodeGenerator} from "./NodeGenerator";
import {ValueProvider} from "../ValueProvider";

export class ObjectTypeGenerator extends NodeGenerator
{
    children:Record<string, NodeGenerator> ={};
    constructor() {
        super(undefined);
    }

    generate(): any {
        const result:any = {};
        for (let childrenKey in this.children) {
            let nodeGen:NodeGenerator = this.children[childrenKey];
            result[childrenKey] = nodeGen.generate();
        }
        return result;
    }

    addChild(name: string, node: NodeGenerator) {
        this.children[name] = node;
    }

}