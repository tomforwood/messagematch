package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.forwoods.messagematch.junit.MessageSpecExtension.LAST_USED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MessageSpecExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class MessageSpecExtensionTest {

    @MockMap
    Map<Class<?>, Object> mocks;

    @Test
    public void testLoad(@MessageSpec("src/test/resources/fish") TestSpec spec) {
        @SuppressWarnings("unused") List<?> list = mock(List.class);
        assertNotNull(spec);
        assertNotNull(mocks);
        assertEquals(1, mocks.size());
        spec.resolve(null);
    }

    @AfterAll
    public static void testLastRunFile() throws IOException {
        File f = new File ("src/test/resources/.fish"+LAST_USED);
        assertTrue(f.exists());
        ZonedDateTime time = ZonedDateTime.parse(Files.readString(f.toPath()));
        long secs = ChronoUnit.SECONDS.between(time, ZonedDateTime.now());
        assertTrue(secs<10);
    }

    @Test
    public void testOther() {
        System.out.println("other");
    }
}