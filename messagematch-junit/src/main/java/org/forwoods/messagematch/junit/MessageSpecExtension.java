package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.util.ClasspathURLStreamHandlerProvider;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.mockito.Mockito;
import org.mockito.listeners.MockCreationListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;

import static org.forwoods.messagematch.spec.TestSpec.TEST_SPEC;

public class MessageSpecExtension implements ParameterResolver,
        AfterTestExecutionCallback,
        BeforeTestExecutionCallback,
        BeforeAllCallback,
        AfterAllCallback{
    public static final String LAST_USED = ".lastUsed";
    Map<String, String> testFiles = new HashMap<>();


    private final Map<Class<?>, Object> mocks = new HashMap<>();
    MockCreationListener mockCreationListener = (mock, settings) -> mocks.put(settings.getTypeToMock(), mock);

    @Override
    public void beforeAll(ExtensionContext context) {
        try {
            URL.setURLStreamHandlerFactory(new ClasspathURLStreamHandlerProvider());
        }
        catch (Error ignored){}
         Mockito.framework().addListener(mockCreationListener);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        Mockito.framework().removeListener(mockCreationListener);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        Object testClass = context.getRequiredTestInstance();
        Class<?> tc = testClass.getClass();
        List<Field> annotatedFields = AnnotationSupport.findAnnotatedFields(tc, MockMap.class);
        for (Field f:annotatedFields) {
            f.set(testClass, mocks);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        String testId = context.getUniqueId();
        String testFileName = testFiles.get(testId);
        if (context.getExecutionException().isEmpty() && testFileName!=null) {
            File testFile = new File(testFileName);
            File directory = testFile.getParentFile();
            String name = testFile.getName()+TEST_SPEC;
            name = name.replace(TEST_SPEC, LAST_USED);
            name = "."+name;
            File lastUsed = new File(directory,name);
            try (FileOutputStream fout = new FileOutputStream(lastUsed))
            {
                fout.write(ZonedDateTime.now().toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.isAnnotated(MessageSpec.class)
                && parameterContext.getParameter().getType().equals(TestSpec.class));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Optional<MessageSpec> findAnnotation = parameterContext.findAnnotation(MessageSpec.class);
        if (findAnnotation.isPresent()) {
            MessageSpec specAnnotation = findAnnotation.get();
            if (!Objects.equals(specAnnotation.value(), "")) {
                String testId = extensionContext.getUniqueId();
                testFiles.put(testId, specAnnotation.value());
                return loadSpec(specAnnotation.value());
            }
        }
        throw new ParameterResolutionException("Cant load spec");
    }

    private TestSpec loadSpec(String value) throws ParameterResolutionException{
        File f = new File(value+TEST_SPEC);
        if (!f.exists()) {
            throw new ParameterResolutionException("Cannot find file with absolute path "+f.getAbsolutePath());
        }
        try {
            TestSpec result = TestSpec.specParser.readValue(f, TestSpec.class);
            result.resolve(f.toURI().toURL());
            return result;
        } catch (IOException e) {
            throw new ParameterResolutionException("Cannot read spec "+value, e);
        }
    }
}
