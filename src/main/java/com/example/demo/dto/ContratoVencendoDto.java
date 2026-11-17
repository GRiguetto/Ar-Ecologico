package com.example.demo.dto;

import java.time.LocalDate;

/** Uma linha da view VW_CONTRATOS_VENCENDO_30_DIAS. */
public record ContratoVencendoDto(
        Integer codContrato,
        String descricao,
        String nomeCliente,
        LocalDate dataFim,
        Integer diasRestantes) {
}
