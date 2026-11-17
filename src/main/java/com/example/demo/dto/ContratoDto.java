package com.example.demo.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entrada para criar/atualizar contrato.
 * Informe exatamente um entre pessoaFisicaCod e pessoaJuridicaCod (código da pessoa).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ContratoDto {
    private String descricao;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    private String tipoContrato;

    private String obs;

    /** 1 = ativo, 2 = encerrado/inativo. Padrão 1. */
    private Integer status;

    private Integer pessoaFisicaCod;

    private Integer pessoaJuridicaCod;
}
