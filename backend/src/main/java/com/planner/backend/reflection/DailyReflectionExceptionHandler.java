package com.planner.backend.reflection;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DailyReflectionController.class)
public class DailyReflectionExceptionHandler {
    // validation errors handled globally by Spring
}
