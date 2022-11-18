package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

public class MethodCallChannel implements Channel{

    final String className;
    final String methodName;
    final String[] methodArgs;
    private final String returnType;

    public MethodCallChannel(@JsonProperty("className") String className,
                             @JsonProperty("methodName") String methodName,
                             @JsonProperty("paramTypes") String[] methodArgs,
                             @JsonProperty("returnType") String returnType) {
        this.className = className;
        this.methodName = methodName;
        this.methodArgs = methodArgs;
        this.returnType = returnType;
    }

    public MethodCallChannel(String className,
                             String methodName,
                             String[] methodArgs) {
        this(className, methodName, methodArgs, null);
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

    public String getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        return "MethodCall{" + className + '.' +
                methodName + '(' +
                Arrays.toString(methodArgs) +
                ')';
    }
}
