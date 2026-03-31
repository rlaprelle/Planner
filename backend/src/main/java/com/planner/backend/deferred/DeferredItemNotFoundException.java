package com.planner.backend.deferred;

public class DeferredItemNotFoundException extends RuntimeException {
    public DeferredItemNotFoundException(String message) {
        super(message);
    }
}
