## Collect tests run in API ##
 - Each test uses APIScenario annotation to inject api scenario object
 - annotation implement param resolver and after each
   - the param resolver finds the scenario file and injects it
   - the after each on success registers a successful run of this file in a static map
 - A successful API run consists of name if executing test, scenario file name, scenario hash, api version (optional) and timestamp
 - A TestExecutionListener writes the successful api test records to a file
 - A separate process TestUploader uploads the file content to a server - probably overwriting the api version

## Collect tests from clients ##
??
basically the same thing?
I guess I dont care if the test passes of fails - it is enough tht it was used
Probably dont have the things that link together all tests though
