package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha da function de tabela FC_HISTORICO_SERVICOS(@NOME). */
public record HistoricoServicoDto(
        Integer codOs,
        String nomeCliente,
        String cidade,
        String documento,
        String tipoOs,
        String status,
        LocalDate dataAbertura,
        LocalDate dataExecucao,
        BigDecimal valor,
        String descricaoProblema,
        String oQueFoiFeito) {
}
