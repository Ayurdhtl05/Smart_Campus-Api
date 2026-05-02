package com.westminster.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * JAX-RS filter implementing both ContainerRequestFilter and
 * ContainerResponseFilter to provide API observability.
 *
 * Logs the HTTP method and URI for every incoming request,
 * and logs the final HTTP status code for every outgoing response.
 *
 * Using JAX-RS filters for cross-cutting concerns like logging is
 * advantageous because:
 *   - It avoids code duplication across resource methods
 *   - Business logic stays clean and focused on domain tasks
 *   - Logging format can be changed in one place
 *   - New endpoints automatically get logged without extra code
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Called for every incoming request.
     * Logs the HTTP method and the full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format("--> Request: %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /**
     * Called for every outgoing response.
     * Logs the HTTP method, URI, and the response status code.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format("<-- Response: %s %s | Status: %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
