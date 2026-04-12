package com.echel.planner.backend.common;

/**
 * Thrown when an operation cannot proceed because the entity is in a conflicting state
 * (e.g., a time block that has already been started).
 */
public class StateConflictException extends RuntimeException {
    public StateConflictException(String message) {
        super(message);
    }
}
