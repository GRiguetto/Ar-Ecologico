package com.example.demo.exception;

/** Registro não encontrado. O GlobalExceptionHandler responde 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

}
