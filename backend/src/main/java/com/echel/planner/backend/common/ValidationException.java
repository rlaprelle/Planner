package com.echel.planner.backend.common;

/** Thrown when a request is syntactically valid but violates a business rule. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
