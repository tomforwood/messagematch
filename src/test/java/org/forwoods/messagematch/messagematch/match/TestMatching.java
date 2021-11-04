package org.forwoods.messagematch.messagematch.match;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.forwoods.messagematch.messagematch.match.JsonMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.Scanners;

class TestMatching {

	static MatchingTest[] tests = new MatchingTest[] { 
			new MatchingTest("int-type", "int-type", true),
			new MatchingTest("int-type", "int-type-fail", false, "[Error at root:value expected matching $Int but was 1.0]"),
			new MatchingTest("regexp-basic", "regexp-basic-pass", true),
			new MatchingTest("regexp-basic", "regexp-basic-fail", false, "[Error at root:value expected matching $^[\\^0-9]*^,abc but was 0]"),
			new MatchingTest("bounds", "int-type", true),
			new MatchingTest("greater-than", "int-type", true)};

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
		assertEquals(expected, jsonMatcher.matches());
		if (r.error!=null) {
			assertEquals(r.error, jsonMatcher.getErrors().toString());
		}
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
