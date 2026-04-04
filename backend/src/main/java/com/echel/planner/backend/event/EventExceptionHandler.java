package com.echel.planner.backend.event;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps event-specific exceptions to RFC 7807 problem details. */
@RestControllerAdvice(assignableTypes = EventController.class)
public class EventExceptionHandler {

    @ExceptionHandler(EventService.EventNotFoundException.class)
    ProblemDetail handleNotFound(EventService.EventNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EventService.EventValidationException.class)
    ProblemDetail handleValidation(EventService.EventValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
