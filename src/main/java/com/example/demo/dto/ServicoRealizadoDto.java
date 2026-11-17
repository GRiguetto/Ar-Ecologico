package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Entrada para lançar um serviço realizado em uma ordem de serviço. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ServicoRealizadoDto {
    private Integer osCod;

    private Integer servicoCod;

    private Integer qtd;

    private BigDecimal valor;

    /** Padrão 0. Não pode ser maior que qtd * valor. */
    private BigDecimal desconto;

    private String obs;

    /** Ex.: PIX, Cartão, Dinheiro. */
    private String formaPagamento;
}
