# messagematch-sampleproject2

This application isn't designed to run but to show how messagematch can be used to test a microservice that relies on another microservice (the sample service defined in the neighbouring maven module).

It defines a standard REST endpoint and then some TestSpec tests of the endpoint. There are a couple of deliberate errors in the implementation though, greeting has been misspelled as greebing

Everything in tghis project is internally consistent, the typo is in a file that duplicates a call in the sample 1 project.

The only way it can be detected it using the message-match-maven plugin which checks the erroneous call against the published api of the sample 1 project.

This results in an error (currently only log rather than build fail) in the maven build.