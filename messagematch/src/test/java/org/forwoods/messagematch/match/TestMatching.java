package org.forwoods.messagematch.match;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TestMatching {

	static MatchingTest[] tests = new MatchingTest[] { 
			new MatchingTest("int-type", "int-type", true),
			new MatchingTest("types", "types", true),
			new MatchingTest("int-type", "int-type-fail", false, "[Error at root:value expected matching $Int but was 1.0]"),
			new MatchingTest("regexp-basic", "regexp-basic-pass", true),
			new MatchingTest("regexp-basic", "regexp-basic-fail", false, "[Error at root:value expected matching $^[\\^0-9]*^,abc but was 0]"),
			new MatchingTest("comparators", "comparators", true),
			new MatchingTest("binding", "binding-pass", true),
			new MatchingTest("binding", "binding-fail", false, "[Error at root:value2 expected matching $Int>5=myVar but was 7]"),
			new MatchingTest("binding-bounds", "binding-bounds-pass", true),
			new MatchingTest("binding-bounds", "binding-bounds-fail", false, "[Error at root:value2 expected matching $Int>$myVar but was 6]"),
			new MatchingTest("binding-unbound", "binding-bounds-pass", false, "[Error at root:value2 expected matching myVar2 to be bound but was unbound]"),
			new MatchingTest("time", "time", true),
			new MatchingTest("time", "int-type", false, "[Error at root expected matching currentms but was not present]"),
			new MatchingTest("strict", "binding-pass",false, "[Error at root expected matching no additional values but was [value2]]"),
			new MatchingTest("array", "array", true),
			new MatchingTest("wildkeys", "wildkeys", true),
			new MatchingTest("array-basic", "array-basic", true),
			new MatchingTest("array-size", "array", true),
			new MatchingTest("int-type", "int-type-null", false, "[Error at root:value expected matching $Int but was null]"),
			new MatchingTest("int-type-nullable", "int-type-null", true),
			};

	@ParameterizedTest
	@MethodSource("getFiles")
	void matcherTests(MatchingTest r) throws IOException {
		String concrete = "concrete/"+r.concreteFile +".json";
		InputStream cin = TestMatching.class.getClassLoader().getResourceAsStream(concrete);

		String matcher = "matchers/"+r.matchFile+".json";
		InputStream min = TestMatching.class.getClassLoader().getResourceAsStream(matcher);
		boolean expected = r.expectsMatch;
		if (min==null) throw new FileNotFoundException("cannot read matcher file "+matcher);
		if (cin==null) throw new FileNotFoundException("cannot read concrete file "+concrete);
		JsonMatcher jsonMatcher = new JsonMatcher(min, cin);
		jsonMatcher.matchTime = 1636044195000L;
		boolean matches = jsonMatcher.matches();
		if (r.error!=null || !jsonMatcher.getErrors().isEmpty()) {
			assertEquals(r.error, jsonMatcher.getErrors().toString());
		}
		assertEquals(expected, matches);
	}

	static Stream<MatchingTest> getFiles() {
		return Arrays.stream(tests);

	}

	static class MatchingTest {
		String matchFile;
		String concreteFile;
		boolean expectsMatch;
		String error;

		public MatchingTest(String matchFile, String concreteFile, boolean expectsMatch) {
			this.matchFile = matchFile;
			this.concreteFile = concreteFile;
			this.expectsMatch = expectsMatch;
		}

		public MatchingTest(String matchFile, String concreteFile, boolean expectsMatch, String error) {
			this(matchFile, concreteFile, expectsMatch);
			this.error = error;
		}

		@Override
		public String toString() {
			return matchFile + 
					" should " + (expectsMatch?"":"Not ") + "match " +
					concreteFile;
		}
		
		

	}

}
