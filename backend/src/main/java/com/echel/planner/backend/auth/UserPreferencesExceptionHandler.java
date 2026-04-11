package com.echel.planner.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserPreferencesExceptionHandler {

    @ExceptionHandler(UserPreferencesService.PreferencesValidationException.class)
    ProblemDetail handleValidation(UserPreferencesService.PreferencesValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
