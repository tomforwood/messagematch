package org.forwoods.messagematch.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.match.MatchError;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestGenerate {

	static final GenerateTest[] tests = new GenerateTest[] {
			new GenerateTest("int-type", "int-type"),
			new GenerateTest("regexp-basic", "regexp-basic-pass"),
			new GenerateTest("comparators", "comparators-gened"),
			new GenerateTest("binding", "binding-pass"),
			new GenerateTest("types", "types"),
			new GenerateTest("array", "array"),
			new GenerateTest("wildkeys", "wildkeys-generated"),
			new GenerateTest("time", "time-gen"),
			new GenerateTest("array-size", "array")
		};
	static ObjectMapper mapper;
	
	@BeforeAll
	static void init() {
		mapper = new ObjectMapper();
	}

	@ParameterizedTest
	@MethodSource("getFiles")
	void generateTests(GenerateTest r) throws IOException {
		String concreteFile = "concrete/"+r.concreteFile +".json";
		InputStream cin = TestGenerate.class.getClassLoader().getResourceAsStream(concreteFile);

		String matcherFile = "matchers/"+r.matchFile+".json";
		InputStream min = TestGenerate.class.getClassLoader().getResourceAsStream(matcherFile);
		if (min==null) throw new FileNotFoundException("cannot read matcher file "+matcherFile);
		if (cin==null) throw new FileNotFoundException("cannot read concrete file "+concreteFile);
		JsonGenerator jsonGenerator = new JsonGenerator(min);
		jsonGenerator.genTime = 1636044195000L;
		JsonNode node = jsonGenerator.generate();
		JsonNode expected = mapper.readTree(cin);
		
		JsonMatcher matcher = new JsonMatcher(expected, node);//notice order flipping here
		boolean matches = matcher.matches();
		assertTrue(matches, ()-> matcher.getErrors().stream().map(MatchError::toString).collect(Collectors.joining("\n")));
	}

	static Stream<GenerateTest> getFiles() {
		return Arrays.stream(tests);
	}

	@Test
	public void testObjectRef() throws IOException {
		String matcher = "matchers/"+"objects.json";
		InputStream min = TestGenerate.class.getClassLoader().getResourceAsStream(matcher);
		HashMap<String, Object> bindings = new HashMap<>();
		JsonGenerator jsonGenerator = new JsonGenerator(min, bindings);
		JsonNode node = jsonGenerator.generate();
		assertEquals("{\"boundObject\":{\"fish\":\"trout\"},\"boundField\":null}", node.toString());

		JsonNode myObj = JsonGenerator.mapper.readTree("{\"fish\":\"herring\"}");
		bindings.put("myObj", myObj);

		min = TestGenerate.class.getClassLoader().getResourceAsStream(matcher);
		jsonGenerator = new JsonGenerator(min, bindings);
		node = jsonGenerator.generate();
		assertEquals("{\"boundObject\":{\"fish\":\"herring\"},\"boundField\":\"herring\"}", node.toString());
	}

	static class GenerateTest {
		final String matchFile;
		final String concreteFile;

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
