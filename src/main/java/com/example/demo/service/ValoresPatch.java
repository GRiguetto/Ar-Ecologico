package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.example.demo.exception.RegraNegocioException;

/**
 * Conversões dos valores recebidos em PATCH (Map&lt;String, Object&gt; vindo do JSON).
 * Valores inválidos geram RegraNegocioException (400) em vez de ClassCastException (500).
 */
final class ValoresPatch {

    private ValoresPatch() {
    }

    static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    static Integer inteiro(String campo, Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(valor.toString().trim());
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Campo '" + campo + "' deve ser um número inteiro");
        }
    }

    static BigDecimal decimal(String campo, Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return new BigDecimal(valor.toString().trim());
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Campo '" + campo + "' deve ser um número");
        }
    }

    static LocalDate data(String campo, Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return LocalDate.parse(valor.toString());
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Campo '" + campo + "' deve estar no formato AAAA-MM-DD");
        }
    }

    static Boolean booleano(String campo, Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Boolean b) {
            return b;
        }
        String s = valor.toString().trim();
        if (s.equalsIgnoreCase("true") || s.equals("1")) {
            return true;
        }
        if (s.equalsIgnoreCase("false") || s.equals("0")) {
            return false;
        }
        throw new RegraNegocioException("Campo '" + campo + "' deve ser true ou false");
    }
}
