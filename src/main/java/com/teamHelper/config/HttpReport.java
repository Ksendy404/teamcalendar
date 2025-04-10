package com.teamHelper.config;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpReport extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "REPORT";

    public HttpReport() {
        super();
    }

    public HttpReport(final URI uri) {
        super();
        setURI(uri);
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}