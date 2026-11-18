package com.example.demo.dto;

import java.math.BigDecimal;

/** Uma linha da function de variável tipo tabela FC_RESUMO_PRESTADORES(@inicio, @fim). */
public record ResumoPrestadorDto(
        Integer prestadorCod,
        String nomePrestador,
        Integer qtdConcluidas,
        BigDecimal faturamento) {
}
