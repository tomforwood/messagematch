# messagematch-sample-project

This project exists to test, not to actually run - it won't start because the non-mock wirings don't exist

It provides some simple REST style enpoints and provides a messagematch based test for *one* of them
The fact that the other is untested should show up using code coverage based tools

It also has a redundant unused test spec, which moreover does not match the OpenApi spcification it is supposed to

This results in errors in the maven build

There is also currently an actual bug in the maven build - the open api spec file is generated after the phase where it is copied onto the classpath so the first install after a clean will fail 
