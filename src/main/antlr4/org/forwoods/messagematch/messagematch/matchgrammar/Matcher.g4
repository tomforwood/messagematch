grammar Matcher;

@header {
}

INT:'$Int';
NUM:'$Num';
STRING:'$String';
INSTANT:'$Instant';
TIME:'$Time';
DATE:'$Date';


RE : '^'(RESC | RSAFE)* '^' ;

NUMBER : '-'?[0-9]+('.'[0-9]+)?EXP?;

IDENTIFIER : [a-zA-Z][a-zA-z0-9]*;

multMatcher: matcher (LineFeed matcher)*;

matcher: (
	typeMatcher | regexpMatcher | boundsMatcher | identifierMatcher
) ;

typeMatcher : type=(INT|NUM|STRING|INSTANT|TIME|DATE) binding? genValue?;

regexpMatcher :  '$' RE binding? genValue;

boundsMatcher : '$' (op=('<'|'<='|'>'|'>=') val=numOrVar |
				(op='+-' '(' val=numOrVar ',' eta=NUMBER ')'))  binding? genValue?;
				
numOrVar : (NUMBER|variable);

identifierMatcher : variable;

variable: '$' IDENTIFIER;

genValue : ',' (REST+|NUMBER|IDENTIFIER) ;

binding : '=' IDENTIFIER;

LineFeed : [\r\n];

REST : ~ [\r\n]+?;

fragment RESC : '\\^';

fragment RSAFE : ~ [^];

fragment EXP
   : [Ee] [+\-]? INT
   ;
   



