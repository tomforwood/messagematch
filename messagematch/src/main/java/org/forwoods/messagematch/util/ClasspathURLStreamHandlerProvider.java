package org.forwoods.messagematch.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

public class ClasspathURLStreamHandlerProvider extends URLStreamHandlerProvider {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("classpath".equals(protocol)) {
            return new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    URL resource = Thread.currentThread().getContextClassLoader().getResource(u.getPath());
                    if (resource==null) {
                        throw new RuntimeException("Could not read resource "+u);
                    }
                    return resource.openConnection();
                }
            };
        }
        return null;
    }

}