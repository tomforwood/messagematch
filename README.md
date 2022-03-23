# messagematch

Message match is designed for testing message driven systems such as microservices.

It consists of libraries for 
 - matching and generating JSON formatted messages and define behaviour [MessageMatch](messagematch/README.md)
 - facilitating the use of defined behavior in JUnit tests and mocking classes *messagematch-junit*
 - validating defined behaviour against tests run and api's defined as openapi *messagematch-maven-plugin*
 - using defined behavior to mock mongo *messagematch-mongo*

Messagematch is designed to be used as part of a maven and JUnit based build as an integration/contract testing framework. It is built around a *TestSpec* defined as YAML in a .testSpec file

## TestSpec
A TestSpec defines the behavior for a single integration level test as a set of calls. the calls can be defined inline or as a reference (currently either a classpath reference or a reference relative to the testSpec). The *callUnderTest* the call that in being tested by this integration test, the *sideEffects* define other calls that will be made as part of the test. the side effects can specify a *times* setting the minimum and maximum number of times a call will be made. By default, a side effect cal is optional.

A call consists of a *channel* specifying how the functionality being tested is expected to be located e.g. a GET call to a uri with value "/users" and the input and expected output to the method as [messageMatch messages](messagematch/README.md)

## Integration testing
To use a testSpec inside a Junit test you should use the JUnit plugin to load a test file as a parameter to a test. Mocks (mockito) should be created for as usual at the edge of your system under test e.g. mocks for DAOs, httpClient, messaging middleware

Athe the start of each test method BehaviourBuilder classes can be used to setup behaviour for mocks. Currently, two behaviour providers are implemented, a MockBehaviourProvider which can be used to mock any method call that takes jackson serializable objects as inputs and outputs and a MongoBehaviourProvider that mocks a MongoCollection, simply instantiate the builder and feed in the collection of side effect calls.

Before use - in the test class initialiser the mongo behaviour builder need to be initialised. E.g.
```
@BeforeAll
    public static void initialise() {
        MongoChannel c = new MongoChannel();
    }
```

The test will then trigger the call under test e.g. using Spring's MockMVCController. The result is compared using a JsonMatcher to the expected result defined in the test.

Finally, the verifyBehaviour on the BehaviourBuilders can be called ot verify that required side effect calls were made.

## Contract testing
The standard way to create a call spec is to have the call under test in a separate file (normally ending callExample) imported into a test spec. For the side effect calls: calls to services you don't control e.g. databases etc. can be specified inline or as relative links for re-use. Calls to services you control are expected to be classpath links to a callExample used in the service being called. By loading the behaviour for your mocks directly from tests of the mocked service you can guarantee data level compatibility between different services.

The standard way to pull in resources created in another project (schema files, call examples) is by putting them on the classpath. Option 1: as standard maven dependencies e.g. depending on test-jar of a dto module of the other project. Option 2: If you don't want the coupling between your two services to be explicit then the required files can be brought in using the maven-dependency-plugin unpack goal as demonstrated in the sample2 project.

## Maven plugin

The maven pugin that attempts to verify the completeness of your event specification testing and verifying agains OpenApi specifications. It ensures that every testSpec in the project has been used by at least one unit test thus ensuring there are no stale tests in your code that might unwittingly be relied upon by consumers of your service

It also checks that every endpoint defined in your openapi specification has at least one test spec that covers it. finally it ensures that any calls that reference external openapi specifications do in fact match that specification





