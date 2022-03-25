## Configuration of the messagematch maven plugin

### resourceDirs  
_default ${project.testResources}_  
directory to scan for testSpecs to check, normally the maven default is the right value

### timestampString
The time that this build started, should always be set to ${maven.build.timestamp}

### openApiFiles
List of open api files for this project to check that all endpoints are covered e.g. 
```xml
<openApiFiles>
    <openApi>target/swagger/sample-swagger.yaml</openApi> 
</openApiFiles>
```

### channelClasses
List of classes to be dynamically loaded for custom channel types e.g.
```xml
<channelClasses>org.forwoods.messagematch.mongo.MongoChannel</channelClasses>
```

### excludedPaths
List of api paths that will not be checked for test coverage in glob style e.g. to avoid checking the swagger endpoints themselves
```xml
<excludePaths>
    <excluded>/swagger-resources/**</excluded>
    <excluded>/swagger-resources</excluded>
</excludePaths>
```

## valdiationLevels
Used to override teh default behavior of the validator
```xml
<validationLevels><!--override the default level of fail so this build can pass, and we can test the sample 2 project-->
    <MISSMATCHED_SPEC>ERROR</MISSMATCHED_SPEC>
</validationLevels>
```

the validation flags defined are
 - UNUSED_SPEC default ERROR,
 - UNTESTED_ENDPOINT default WARN,
 - MISSMATCHED_SPEC default FAIL;

Warn logs to the maven log at warn level, error logs at error levvel and fail logs at error and causes the build to fail.