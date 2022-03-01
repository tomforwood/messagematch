package org.forwoods.messagematch.matchgrammar;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GrammarTest {

	@ParameterizedTest
	@ValueSource(strings = {
			"$Int",
			"$Int,5",
			"$^[\\^a-z]^,a", 
			"$Int+-(2,1.0)",
			"$Int>1",
			"$Int=myInt",
			"$^[\\\\^a-z]^=myString,a",
			"$Int>myInt",
			"$Int>myVar2"})
	void sucessTests(String toParse) {
		MatcherLexer l = new MatcherLexer(CharStreams.fromString(toParse));
		MatcherParser p = new MatcherParser(new CommonTokenStream(l));
		p.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
	    p.matcher();
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"$^ab^"})
	void failtests(String toParse) {
		MatcherLexer l = new MatcherLexer(CharStreams.fromString(toParse));
		MatcherParser p = new MatcherParser(new CommonTokenStream(l));
		p.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
	    assertThrows(IllegalStateException.class, ()-> p.matcher());
	}
	

}
