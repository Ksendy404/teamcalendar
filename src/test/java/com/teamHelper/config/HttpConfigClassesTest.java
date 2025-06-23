
package com.teamHelper.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class HttpConfigClassesTest {

    @Test
    void shouldCreateHttpPropfindWithUri() {
        URI uri = URI.create("http://localhost/test");
        HttpPropfind propfind = new HttpPropfind(uri);
        assertEquals("PROPFIND", propfind.getMethod());
    }

    @Test
    void shouldCreateHttpReportWithUri() {
        URI uri = URI.create("http://localhost/test");
        HttpReport report = new HttpReport(uri);
        assertEquals("REPORT", report.getMethod());
    }
}
