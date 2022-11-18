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

IDENTIFIER : [a-zA-Z][a-zA-Z0-9]*;

multMatcher: matcher (LineFeed matcher)*;

matcher: (
	typeMatcher | regexpMatcher
) ;

typeMatcher : type=(INT|NUM|STRING|INSTANT|TIME|DATE) (nullable='?')? (comp = comparator)? binding? genValue?;

regexpMatcher :  '$' RE binding? genValue;


comparator : (op=('<'|'<='|'>'|'>=') val=valOrVar) |
            (op=('+-'|'++') '(' val=valOrVar ',' eta=NUMBER ')');
				
valOrVar : (literal|variable);

variable: '$' IDENTIFIER;

genValue : ',' literal ;

literal : (REST+|NUMBER|IDENTIFIER)*;

binding : '=' IDENTIFIER;

LineFeed : [\r\n];

REST : ~ [\r\n]+?;

fragment RESC : '\\^';

fragment RSAFE : ~ [^];

fragment EXP
   : [Ee] [+\-]? INT
   ;
   



