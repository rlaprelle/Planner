package com.echel.planner.backend.common;

/** Thrown when a requested entity does not exist or is not accessible to the current user. */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
