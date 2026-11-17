package com.example.demo.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "senha")
@Builder
public class PrestadorServicoDto {
    private String endereco;

    private String cep;

    private String numero;

    private String cidade;

    private String estado;

    private String telefone;

    private String nome;

    private String email;

    private String cpf;

    private String rg;

    private LocalDate dataNascimento;

    /** Coluna PAGAMENTO. Também aceita o nome antigo "salario". */
    @JsonAlias("salario")
    private BigDecimal pagamento;

    private Boolean administrador;

    /** Senha em texto; o back-end grava apenas o hash BCrypt. */
    private String senha;
}
