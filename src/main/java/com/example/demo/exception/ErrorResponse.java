package com.example.demo.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String status;

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, "error");
    }
}