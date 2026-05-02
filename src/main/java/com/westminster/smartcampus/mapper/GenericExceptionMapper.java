package com.westminster.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "catch-all" ExceptionMapper for Throwable.
 *
 * This is the safety net that intercepts any unexpected runtime errors
 * (e.g., NullPointerException, IndexOutOfBoundsException) and returns
 * a generic HTTP 500 Internal Server Error with a sanitised JSON body.
 *
 * The full stack trace is logged server-side for operators but is NEVER
 * exposed to the client. Leaking stack traces is a security risk because
 * they reveal framework versions, class names, file paths, and database
 * details that an attacker could exploit.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // If this is a JAX-RS WebApplicationException with its own response,
        // honour it (but still wrap it in JSON format)
        if (ex instanceof WebApplicationException) {
            Response existing = ((WebApplicationException) ex).getResponse();
            if (existing != null && existing.getStatus() != 500) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", existing.getStatus());
                body.put("error", existing.getStatusInfo().getReasonPhrase());
                body.put("message", ex.getMessage() != null ? ex.getMessage() : "Request could not be processed");
                return Response.status(existing.getStatus())
                        .type(MediaType.APPLICATION_JSON)
                        .entity(body)
                        .build();
            }
        }

        // Log the full stack trace server-side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GenericExceptionMapper: "
                                 + ex.getClass().getName(), ex);

        // Return a sanitised, generic error to the client — no stack trace leaked
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 500);
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the administrator.");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
