package com.auth.exceptions;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public AuthException(String message) {
        this(message, HttpStatus.BAD_REQUEST, null);
    }

    public AuthException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public AuthException(String message, HttpStatus status, String codigo) {
        super(message);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }
}