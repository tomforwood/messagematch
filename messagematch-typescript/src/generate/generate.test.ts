import { expect, test, describe } from 'vitest';
import { readFileSync } from 'fs';

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