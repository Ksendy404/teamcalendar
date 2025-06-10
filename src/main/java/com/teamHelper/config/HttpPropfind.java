package com.teamHelper.config;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpPropfind extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "PROPFIND";

    public HttpPropfind() {
        super();
    }

    public HttpPropfind(final URI uri) {
        super();
        setURI(uri);
    }
    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}