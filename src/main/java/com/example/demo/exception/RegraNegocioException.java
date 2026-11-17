package com.example.demo.exception;

/** Dados de entrada que violam uma regra de negócio. O GlobalExceptionHandler responde 400. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }

}
