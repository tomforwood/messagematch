import {JsonMatcher} from "./JsonMatcher";

export class PathMatcher {
    constructor(private matcherPath:string, private concretePath:string, readonly binding:Record<string, any>) {
    }

    public matches():boolean {
        const pattern = new PathMatcherPattern(this.matcherPath);
        const matcher = this.concretePath.match(pattern.pattern)
        if (matcher) {
            for (let i = 0; i < pattern.bindingExpressions.length; i++) {
                const bindingExpression = pattern.bindingExpressions[i];
                const fieldMatcher = JsonMatcher.parseMatcher(bindingExpression);
                const concrete = matcher[i+1];
                if (!fieldMatcher.matches(concrete, this.binding)) return false;
            }
            return true;
        }
        return false;
    }
}

const matcherDeconstruct = /\{[^}]+\}/g;

class PathMatcherPattern {
    bindingExpressions:string[] =[];
    pattern:RegExp
    constructor (private matcherPath:string){
        let r:string='^';
        const pathbindings = matcherPath.matchAll(matcherDeconstruct)
        let pos = 0;
        for (const pathBinding of pathbindings){
            r+=matcherPath.substring(pos, pathBinding.index);
            r+="([^/]+)";
            this.bindingExpressions.push(pathBinding[0].substring(1, pathBinding[0].length-1));
            pos = pathBinding.index + pathBinding[0].length
        }
        r+=matcherPath.substring(pos);
        r+='$'
        this.pattern = new RegExp(r);
    }
}