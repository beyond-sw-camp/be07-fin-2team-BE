package com.padaks.todaktodak.common.exception.exceptionType;

import org.springframework.http.HttpStatus;

public interface ExceptionType {
    String name();
    HttpStatus httpStatus();
    String message();
}