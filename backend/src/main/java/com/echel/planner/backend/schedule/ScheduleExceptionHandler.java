package com.echel.planner.backend.schedule;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScheduleExceptionHandler {

    @ExceptionHandler(ScheduleService.ScheduleValidationException.class)
    ProblemDetail handleValidation(ScheduleService.ScheduleValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ScheduleService.BlockNotFoundException.class)
    ProblemDetail handleBlockNotFound(ScheduleService.BlockNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ScheduleService.BlockAlreadyStartedException.class)
    ProblemDetail handleBlockAlreadyStarted(ScheduleService.BlockAlreadyStartedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
