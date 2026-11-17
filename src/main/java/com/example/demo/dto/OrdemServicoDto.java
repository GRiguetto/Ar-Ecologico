package com.example.demo.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entrada para criar/atualizar ordem de serviço.
 * Informe exatamente um entre pessoaFisicaCod e pessoaJuridicaCod.
 * contratoCod e prestadorServicoCod são opcionais.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class OrdemServicoDto {
    private String tipoOs;

    private BigDecimal valor;

    private String obs;

    private LocalDate dataAbertura;

    /** 1 = aberta, 2 = em andamento, 3 = concluída, 4 = cancelada. Padrão 1. */
    private Integer status;

    private String descricaoProblema;

    private String descricaoExecutado;

    private LocalDate dataExecucao;

    private Integer pessoaFisicaCod;

    private Integer pessoaJuridicaCod;

    private Integer contratoCod;

    private Integer prestadorServicoCod;
}
