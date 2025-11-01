import {expect, describe, test} from "vitest";
import {PathMatcher} from "./PathMatcher";

describe('pathmatcher (TS)', () => {
    test('should match a simple path', () => {
        const matcherPath:string = "/storedNumbers";
        const concretePath:string = "/storedNumbers";
        const pathMatcher = new PathMatcher(matcherPath, concretePath, {});
        expect(pathMatcher.matches()).toBeTruthy();
    });

    test('should match Int Path Param', () => {
        const matcherPath:string = "/{$Int=accountId,5}/storedNumbers";
        const concretePath:string = "/7/storedNumbers";
        const pathMatcher = new PathMatcher(matcherPath, concretePath, {});
        expect(pathMatcher.matches()).toBeTruthy();
        expect(pathMatcher.binding["accountId"]).toBe("7");
    });

    test('should match complex Matcher and bind', () => {
        const matcherPath:string = "/travel/{$String=from}-{$String=to}";
        const concretePath:string = "/travel/London-Edinburgh";
        const pathMatcher = new PathMatcher(matcherPath, concretePath, {});
        expect(pathMatcher.matches()).toBeTruthy();
        expect(pathMatcher.binding["from"]).toBe("London");
        expect(pathMatcher.binding["to"]).toBe("Edinburgh");
    });

    test('should not match', ()=>{
        const matcherPath:string = "/travel/{$String=from}-{$String=to}";
        const concretePath:string = "/traveling/London-Edinburgh";
        const pathMatcher = new PathMatcher(matcherPath, concretePath, {});
        expect(pathMatcher.matches()).toBeFalsy();
    })

    test('should respect existing bindings', ()=>{
        const matcherPath:string = "/travel/{$String=from}-{$String=to}";
        const concretePath:string = "/travel/London-Edinburgh";

        const pathMatcher = new PathMatcher(matcherPath, concretePath, {"from": "Bristol"});
        expect(pathMatcher.matches()).toBeFalsy();
    })

    test('should match whole path', ()=>{
        const matcherPath:string = "/storedNumbers";
        const concretePath:string = "/storedNumbersBroken";
        const pathMatcher = new PathMatcher(matcherPath, concretePath, {});
        expect(pathMatcher.matches()).toBeFalsy();
    })
});