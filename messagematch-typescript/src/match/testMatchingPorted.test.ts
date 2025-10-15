import { expect, test, describe } from 'vitest';
import { readFileSync } from 'fs';
import { JsonMatcher } from './JsonMatcher';

type MatchingTest = { matchFile: string, concreteFile: string, expectsMatch: boolean, error?: string };

const tests: MatchingTest[] = [
    { matchFile: 'int-type', concreteFile: 'int-type', expectsMatch: true },
    { matchFile: 'types', concreteFile: 'types', expectsMatch: true },
    { matchFile: 'int-type', concreteFile: 'int-type-fail', expectsMatch: false, error: 'Error at root:value expected matching $Int but was 1.1' },
    { matchFile: 'regexp-basic', concreteFile: 'regexp-basic-pass', expectsMatch: true },
    { matchFile: 'regexp-basic', concreteFile: 'regexp-basic-fail', expectsMatch: false, error: 'Error at root:value expected matching $^#[\\^0-9]*^,#abc but was 0' },
    { matchFile: 'comparators', concreteFile: 'comparators', expectsMatch: true },
    { matchFile: 'binding', concreteFile: 'binding-pass', expectsMatch: true },
    { matchFile: 'binding', concreteFile: 'binding-fail', expectsMatch: false, error: 'Error at root:value2 expected matching $Int>5=myVar but was 7' },
    { matchFile: 'binding-bounds', concreteFile: 'binding-bounds-pass', expectsMatch: true },
    { matchFile: 'binding-bounds', concreteFile: 'binding-bounds-fail', expectsMatch: false, error: 'Error at root:value2 expected matching $Int>$myVar but was 6' },
    { matchFile: 'binding-unbound', concreteFile: 'binding-bounds-pass', expectsMatch: false, error: 'Error at root:value2 expected matching myVar2 to be bound but was unbound' },
    { matchFile: 'time', concreteFile: 'time', expectsMatch: true },
    { matchFile: 'time', concreteFile: 'int-type', expectsMatch: false, error: 'Error at root expected matching currentms but was not present' },
    { matchFile: 'strict', concreteFile: 'binding-pass', expectsMatch: false, error: 'Error at root expected matching no additional values but was [value2]' },
    { matchFile: 'array', concreteFile: 'array', expectsMatch: true },
    { matchFile: 'wildkeys', concreteFile: 'wildkeys', expectsMatch: true },
    { matchFile: 'array-basic', concreteFile: 'array-basic', expectsMatch: true },
    { matchFile: 'array-size', concreteFile: 'array', expectsMatch: true },
    { matchFile: 'int-type', concreteFile: 'int-type-null', expectsMatch: false, error: 'Error at root:value expected matching $Int but was null' },
    { matchFile: 'int-type-nullable', concreteFile: 'int-type-null', expectsMatch: true },
];

import { resolve } from 'path';

function loadResource(path: string) {
    // repo root is four levels up from this test directory
    const repoRoot = resolve(__dirname, '../../..');
    const full = `${repoRoot}/messagematch/src/test/resources/${path}`;
    return JSON.parse(readFileSync(full, 'utf8'));
}

describe('TestMatching ported (TS)', () => {
    test.each(tests)('Matcher %o', (t) => {
        const matcher = loadResource(`matchers/${t.matchFile}.json`);
        const concrete = loadResource(`concrete/${t.concreteFile}.json`);
        const jm = new JsonMatcher(matcher, concrete);
        // make time deterministic as in Java test
        (jm as any).matchTime = 1636044195000;
        const matches = jm.matches();
        if (t.error || jm.getErrors().length>0) {
            const errors = jm.getErrors().map(e => e.toString());
            expect(errors, errors.toString()).toContain(t.error);
        }
        expect(matches).toBe(t.expectsMatch);
    });

    // test('testObjectRef', () => {
    //     const matcher = loadResource('matchers/objects.json');
    //     const concrete = loadResource('concrete/objects.json');
    //     const jm = new JsonMatcher(matcher, concrete);
    //     const matches = jm.matches();
    //     if (!matches) console.error(jm.getErrors());
    //     expect(matches).toBe(true);
    //     const myObj = jm.getBindings().get('myObj');
    //     expect(String(myObj)).toBe('{"fish":"trout"}');
    // });
});
