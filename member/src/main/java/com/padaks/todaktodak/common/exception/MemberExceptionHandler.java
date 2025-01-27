package com.padaks.todaktodak.common.exception;

import com.padaks.todaktodak.common.exception.exceptionType.ExceptionType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.persistence.EntityNotFoundException;

@RestControllerAdvice
public class MemberExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        ExceptionType exceptionType = e.getExceptionType();

        return ResponseEntity.status(exceptionType.httpStatus())
                .body(ErrorResponse.of(exceptionType));
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(EntityNotFoundException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("EntityNotFoundException",HttpStatus.BAD_REQUEST.value(),e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(IllegalArgumentException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("IllegalArgumentException",HttpStatus.BAD_REQUEST.value(),e.getMessage()));
    }
}