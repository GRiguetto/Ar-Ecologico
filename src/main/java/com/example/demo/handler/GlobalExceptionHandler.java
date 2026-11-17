package com.example.demo.handler;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.exception.ErrorResponse;
import com.example.demo.exception.NaoAutorizadoException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> handleNaoAutorizado(NaoAutorizadoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({RegraNegocioException.class, IllegalArgumentException.class, ClassCastException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Valor inválido para '" + ex.getName() + "': " + ex.getValue());
    }

    /** Violação de constraint do banco: UNIQUE (CPF/CNPJ), CHECK, FK, NOT NULL. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        return resposta(HttpStatus.CONFLICT,
                "Operação viola uma regra do banco: " + ex.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Exceções do próprio Spring MVC (rota inexistente, método não suportado etc.)
        // já carregam o status HTTP correto.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            return resposta(springError.getStatusCode(), ex.getMessage());
        }
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> resposta(HttpStatusCode status, String mensagem) {
        ErrorResponse response = ErrorResponse.builder()
                .message(mensagem)
                .status(String.valueOf(status.value()))
                .build();
        return ResponseEntity.status(status).body(response);
    }

}
