package org.forwoods.messagematch.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

public class ClasspathURLStreamHandlerProvider extends URLStreamHandlerProvider {

    final URLStreamHandler handler = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            URL resource = resolveClasspathURL(u);
            return resource.openConnection();
        }
    };

    public static URL resolveClasspathURL(URL u) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(u.getPath());
        if (resource==null) {
            throw new RuntimeException("Could not read resource "+ u);
        }
        return resource;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("classpath".equals(protocol)) {
            return handler;
        }
        return null;
    }

}