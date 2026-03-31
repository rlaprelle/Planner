package com.planner.backend.deferred;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DeferredItemController.class)
public class DeferredItemExceptionHandler {

    @ExceptionHandler(DeferredItemService.DeferredItemNotFoundException.class)
    ProblemDetail handleNotFound(DeferredItemService.DeferredItemNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
