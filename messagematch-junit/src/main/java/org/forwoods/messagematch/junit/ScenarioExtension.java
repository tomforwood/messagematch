package org.forwoods.messagematch.junit;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.forwoods.messagematch.junit.testrunstore.TestRunStore;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class ScenarioExtension implements ParameterResolver, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ScenarioExtension.class);

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        context.getTestMethod().ifPresent(method -> {
            context.getExecutionException().ifPresentOrElse( e->{},
                    ()->{
                        final APIScenarioLoadDetails apiTestScenario = context.getStore(NAMESPACE).get(method, APIScenarioLoadDetails.class);

                        if (apiTestScenario!=null) {
                            //Check lifecycle to see if we need the parent context;
                            context.getParent().ifPresent(parentContext -> {
                                final SuccessfulScenarioTest testRecord = buildSuccessfulScenarioTest(method, apiTestScenario);
                                TestRunStore.INSTANCE.storeTestRun(testRecord);
                                //parentContext.getStore(NAMESPACE).put(method, testRecord);
                            });
                        }
                    });
        });
    }

    private SuccessfulScenarioTest buildSuccessfulScenarioTest(final Method method, final APIScenarioLoadDetails apiTestScenario) {
        return new SuccessfulScenarioTest(method.toString(), apiTestScenario.path, apiTestScenario.hash, null, Instant.now());
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.isAnnotated(Scenario.class)
                && parameterContext.getParameter().getType().equals(APITestScenario.class));
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
        Optional<Scenario> findAnnotation = parameterContext.findAnnotation(Scenario.class);
        if (findAnnotation.isPresent()) {
            Scenario specAnnotation = findAnnotation.get();
            if (!Objects.equals(specAnnotation.value(), "")) {
                final APIScenarioLoadDetails apiTestScenario = loadSpec(specAnnotation.value());
                System.out.println("Looking for " + specAnnotation.value());
                extensionContext.getTestMethod().ifPresent(method -> {
                    System.out.println("got a method ");
                    final ExtensionContext.Store store = getClassLevelStore(extensionContext);

                    store.put(method, apiTestScenario);
                });
                return apiTestScenario.apiTestScenario;
            }
        }
        throw new ParameterResolutionException("Cant load scenario");
    }

    private APIScenarioLoadDetails loadSpec(final String scenarioName) {
        try(final InputStream resourceAsStream = ScenarioExtension.class.getClassLoader().getResourceAsStream(scenarioName))
        {
            HashingInputStream his = new HashingInputStream(Hashing.crc32(), resourceAsStream);
            final APITestScenario apiTestScenario = APITestScenario.specParser.readValue(his, APITestScenario.class);
            return new APIScenarioLoadDetails(apiTestScenario, scenarioName, Long.toString(his.hash().asInt()));
            //TODO resolving?

        } catch (IOException e) {
            throw new RuntimeException("Could not load Scenario " + scenarioName + " from classpath", e);
        }
    }

    private ExtensionContext.Store getClassLevelStore(ExtensionContext extensionContext) {
        while (extensionContext.getTestInstanceLifecycle().map(l->l != TestInstance.Lifecycle.PER_CLASS).orElse(false)){
            extensionContext = extensionContext.getParent().orElseThrow(()->new BehaviourVerificationException("Could not get the class level test context"));
        }
        return extensionContext.getStore(NAMESPACE);
    }

    private record APIScenarioLoadDetails(APITestScenario apiTestScenario, String path, String hash) {}
}
