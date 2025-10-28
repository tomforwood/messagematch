package org.forwoods.messagematch.generate;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathGeneratorTest
{

    @Test
    public void testGeneratePath()
    {
        String pathTemplate = "/{$Int=accountId,5}/storedNumbers";
        PathGenerator generator = new PathGenerator(pathTemplate, Map.of());
        final String generated = generator.generate();
        assertEquals("/5/storedNumbers", generated);

    }

    @Test
    @Disabled
    public void testGeneratePathWithBinding()
    {
        //TODO This needs work
        //a Bound variable should override a default value
        String pathTemplate = "/{$Int=accountId,5}/storedNumbers";
        PathGenerator generator = new PathGenerator(pathTemplate, Map.of("accountId", 6));
        final String generated = generator.generate();
        assertEquals("/6/storedNumbers", generated);
    }
}
