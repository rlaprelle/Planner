package com.planner.backend.deferred;

import org.springframework.web.bind.annotation.RestControllerAdvice;

// No domain exceptions in this slice.
// Exceptions for convert/defer/dismiss will be added in the Evening Ritual slice.
@RestControllerAdvice
public class DeferredItemExceptionHandler {
}
