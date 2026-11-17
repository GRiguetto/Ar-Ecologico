package com.example.demo.exception;

/** Credenciais inválidas. O GlobalExceptionHandler responde 401. */
public class NaoAutorizadoException extends RuntimeException {

    public NaoAutorizadoException(String message) {
        super(message);
    }

}
