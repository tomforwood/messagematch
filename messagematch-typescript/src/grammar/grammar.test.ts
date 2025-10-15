import { CharStream, CommonTokenStream } from 'antlr4';
import { expect, test } from 'vitest'
import MatcherLexer from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherLexer.js';
import MatcherParser from '../antlr4ts/org/forwoods/messagematch/matchgrammar/MatcherParser.js';
import { describe } from 'node:test';

function parseWithListener(input: string): void {
    //const chars = new (require('antlr4').InputStream)(input);
    const chars = new CharStream(input);
    const lexer = new MatcherLexer(chars);
    const tokens = new CommonTokenStream(lexer);
    const parser = new MatcherParser(tokens);
    // mimic Java BaseErrorListener that throws on syntax error
    parser.removeErrorListeners();
    parser.addErrorListener({
        syntaxError(_recognizer: any, _offendingSymbol: any, line: number, charPositionInLine: number, msg: string) {
            throw new Error(`failed to parse at line ${line} due to ${msg}`);
        }
    } as any);
    parser.matcher();
}
describe('GrammarTest (TS)', () => {
    test.each([
        '$Int',
        '$Int,5',
        "$^[\\^a-z]^,a",
        '$Int+-(2,1.0)',
        '$Int>1',
        '$Int=myInt',
        "$^[\\\\^a-z]^=myString,a",
        '$Int>myInt',
        '$Int>myVar2'
    ])('Should be valid %s', expr => {
        expect(() => parseWithListener(expr)).not.toThrow();
    });


test.each([
    '$^ab^'
])('Should be invalid %s', expr => {
    expect(() => parseWithListener(expr)).toThrow();
});
});
