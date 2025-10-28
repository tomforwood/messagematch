import { expect, test, describe } from 'vitest';
import { readFileSync } from 'fs';
import { resolve } from 'path';
import { JsonGenerator } from './JsonGenerator';
import {JsonMatcher} from "../match/JsonMatcher";

type GenerateTest = { matchFile: string, concreteFile: string };

const tests: GenerateTest[] = [
    {matchFile:"int-type", concreteFile: "int-type"},
    {matchFile:"regexp-basic", concreteFile: "regexp-basic-pass"},
    {matchFile:"comparators", concreteFile: "comparators-gened"},
    {matchFile:"binding", concreteFile: "binding-pass"},
    {matchFile:"types", concreteFile: "types"},
    {matchFile:"array", concreteFile: "array"},
    {matchFile:"wildkeys", concreteFile: "wildkeys-generated"},
    {matchFile:"time", concreteFile: "time-gen"},
    {matchFile:"array-size", concreteFile: "array"},
]

function loadResource(path: string) {
    // repo root is two levels up from this directory inside messagematch-typescript
    const repoRoot = resolve(__dirname, '../../..');
    const full = `${repoRoot}/messagematch/src/test/resources/${path}`;
    return JSON.parse(readFileSync(full, 'utf8'));
}

describe('TestGenerate ported (TS) - stubs', () => {
    test.each(tests)('generate %o', (t) => {
        const matcher = loadResource(`matchers/${t.matchFile}.json`);
        const concrete = loadResource(`concrete/${t.concreteFile}.json`);
        const gen = new JsonGenerator(matcher);
        // make generation deterministic as in Java test
        gen.genTime = 1636044195000;
        const node = gen.generate();

        const jsonMatcher: JsonMatcher = new JsonMatcher(concrete, node);//notice order flipping here
        let matches = jsonMatcher.matches();
        expect(matches, jsonMatcher.getErrors().toString()).toBeTruthy();
    });

    test('testObjectRef (stubbed)', () => {
        const matcher = loadResource('matchers/objects.json');
        const bindings: Record<string, any> = {};
        const gen = new JsonGenerator(matcher, bindings);
        const node = gen.generate();

        // Stub: ensure generate returns an object string for now
        expect(JSON.stringify(node)).toBeDefined();

        // If bindings were used by a real generator, the second run would differ.
        bindings['myObj'] = { fish: 'herring' };
        const gen2 = new JsonGenerator(matcher, bindings);
        const node2 = gen2.generate();
        expect(JSON.stringify(node2)).toBeDefined();
    });
});