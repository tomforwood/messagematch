package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MethodCallChannel implements Channel{

    final String className;
    final String methodName;
    final String[] methodArgs;

    public MethodCallChannel(@JsonProperty("className") String className,
                             @JsonProperty("methodName") String methodName,
                             @JsonProperty("paramTypes") String[] methodArgs) {
        this.className = className;
        this.methodName = methodName;
        this.methodArgs = methodArgs;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String[] getMethodArgs() {
        return methodArgs;
    }
}
