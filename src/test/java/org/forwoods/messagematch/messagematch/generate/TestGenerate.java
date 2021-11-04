package org.forwoods.messagematch.messagematch.generate;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.forwoods.messagematch.messagematch.match.JsonMatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.Scanners;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestGenerate {

	static GenerateTest[] tests = new GenerateTest[] { 
			new GenerateTest("int-type", "int-type"),
			new GenerateTest("regexp-basic", "regexp-basic-pass"),
			new GenerateTest("bounds", "int-type"),
			new GenerateTest("greater-than", "int-type")
		};
	static ObjectMapper mapper;
	
	@BeforeAll
	static void init() {
		mapper = new ObjectMapper();
	}

	@ParameterizedTest
	@MethodSource("getFiles")
	void matcherTests(GenerateTest r) throws IOException {
		String concreteFile = "concrete/"+r.concreteFile +".json";
		InputStream cin = TestGenerate.class.getClassLoader().getResourceAsStream(concreteFile);

		String matcherFile = "matchers/"+r.matchFile+".json";
		InputStream min = TestGenerate.class.getClassLoader().getResourceAsStream(matcherFile);
		if (min==null) throw new FileNotFoundException("cannot read matcher file "+matcherFile);
		if (cin==null) throw new FileNotFoundException("cannot read concrete file "+concreteFile);
		JsonGenerator jsonGenerator = new JsonGenerator(min);
		JsonNode node = jsonGenerator.generate();
		JsonNode expected = mapper.readTree(cin);
		
		JsonMatcher matcher = new JsonMatcher(node, expected);
		boolean matches = matcher.matches();
		assertTrue(matches, ()->matcher.getErrors().toString());
	}

	static Stream<GenerateTest> getFiles() {
		return Arrays.stream(tests);

	}

	static class GenerateTest {
		String matchFile;
		String concreteFile;

		public GenerateTest(String matchFile, String concreteFile) {
			super();
			this.matchFile = matchFile;
			this.concreteFile = concreteFile;
		}

		@Override
		public String toString() {
			return matchFile + 
					" should generate " +
					concreteFile;
		}
		
		

	}

}
