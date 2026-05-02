package com.westminster.smartcampus.app;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

/**
 * Bootstraps an embedded Grizzly HTTP server hosting the JAX-RS application.
 * The server starts on http://localhost:8080/ and all API endpoints
 * are available under /api/v1 as configured by SmartCampusApplication.
 */
public class Main {

    public static final String BASE_URI = "http://localhost:9090/api/v1/";

    /**
     * Creates and starts the Grizzly HTTP server with the JAX-RS application.
     *
     * @return the started HttpServer instance
     */
    public static HttpServer startServer() {
        // Create a ResourceConfig from our Application subclass
        ResourceConfig rc = ResourceConfig.forApplication(new SmartCampusApplication());
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main entry point — starts the server and keeps it running until interrupted.
     */
    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.println("================================================");
        System.out.println("  Smart Campus API started successfully!");
        System.out.println("  Base URL: " + BASE_URI);
        System.out.println("  Rooms:    " + BASE_URI + "rooms");
        System.out.println("  Sensors:  " + BASE_URI + "sensors");
        System.out.println("================================================");
        System.out.println("Press Ctrl+C to stop the server...");

        // Keep the main thread alive so the server continues running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.shutdownNow();
        }
    }
}
