package com.westminster.smartcampus.app;

import com.westminster.smartcampus.filter.LoggingFilter;
import com.westminster.smartcampus.mapper.GenericExceptionMapper;
import com.westminster.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.NotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.westminster.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.westminster.smartcampus.resource.DiscoveryResource;
import com.westminster.smartcampus.resource.SensorResource;
import com.westminster.smartcampus.resource.SensorRoomResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application subclass that establishes the versioned API entry point.
 *
 * The @ApplicationPath("/api/v1") annotation sets the base URI for all
 * resource classes registered in this application. Every resource path
 * is relative to /api/v1.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // --- Resource classes ---
        classes.add(DiscoveryResource.class);
        classes.add(SensorRoomResource.class);
        classes.add(SensorResource.class);

        // --- Exception Mappers ---
        classes.add(RoomNotEmptyExceptionMapper.class);          // 409 Conflict
        classes.add(LinkedResourceNotFoundExceptionMapper.class); // 422 Unprocessable Entity
        classes.add(SensorUnavailableExceptionMapper.class);      // 403 Forbidden
        classes.add(NotFoundExceptionMapper.class);               // 404 Not Found
        classes.add(GenericExceptionMapper.class);                // 500 catch-all

        // --- Filters ---
        classes.add(LoggingFilter.class);

        return classes;
    }
}
