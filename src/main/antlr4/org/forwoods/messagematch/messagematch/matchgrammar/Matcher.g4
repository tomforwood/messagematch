grammar Matcher;

@header {
    package org.forwoods.messagematch.messagematch.matchgrammar;
}

DOLLAR:'$';
INT:'Int';
NUM:'Num';
STRING:'String';

RE : '^'(RESC | RSAFE)* '^' ;

NUMBER : '-'?[0-9]+('.'[0-9]+)?EXP?;

IDENTIFIER : [a-zA-Z][a-zA-z0-9]*;

multMatcher: matcher (LineFeed matcher)*;

matcher: DOLLAR (
	typeMatcher | regexpMatcher | boundsMatcher | identifierMatcher
) ;

typeMatcher : type=(INT|NUM|STRING) binding? genValue?;

regexpMatcher :  RE binding? genValue;

boundsMatcher : (op=('<'|'<='|'>'|'>=') val=numOrVar |
				(op='+-' '(' val=numOrVar ',' eta=NUMBER ')'))  binding? genValue?;
				
numOrVar : (NUMBER|variable);

identifierMatcher : IDENTIFIER;

variable: DOLLAR IDENTIFIER;

genValue : ',' (REST+|NUMBER|IDENTIFIER) ;

binding : '=' IDENTIFIER;

LineFeed : [\r\n];

REST : ~ [\r\n]+?;

fragment RESC : '\\^';

fragment RSAFE : ~ [^];

fragment EXP
   : [Ee] [+\-]? INT
   ;
   



