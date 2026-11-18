package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha da view VW_ORDENS_SERVICO_PESSOAS (OS com cliente e prestador). */
public record OrdemServicoPessoaDto(
        Integer codOs,
        String tipoPessoa,
        String nomeCliente,
        String documento,
        String tipoOs,
        String status,
        LocalDate dataAbertura,
        BigDecimal valor,
        String descricaoProblema,
        String oQueFoiFeito,
        String nomePrestador) {
}
