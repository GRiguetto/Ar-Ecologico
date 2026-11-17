package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Entrada para associar um equipamento a um contrato. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ContratoEquipamentoDto {
    private Integer contratoCod;

    private Integer equipamentoCod;

    private Integer qtd;
}
