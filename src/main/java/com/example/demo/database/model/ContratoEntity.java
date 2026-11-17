package com.example.demo.database.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@AllArgsConstructor 
@NoArgsConstructor 
@Entity 
@Table(
    name = "CONTRATOS",
    check = {
        @CheckConstraint(
            name = "CK_CONTRATO_STATUS",
            constraint = "STATUS IN (1, 2)"
        ),
        @CheckConstraint(
            name = "CK_CONTRATO_PF_OU_PJ",
            constraint = "(COD_PESSOA_FISICA IS NOT NULL AND COD_PESSOA_JURIDICA IS NULL) " +
                         "OR (COD_PESSOA_FISICA IS NULL AND COD_PESSOA_JURIDICA IS NOT NULL)"
        )
    }
)
@ToString(exclude = {"pessoaFisicaCod", "pessoaJuridicaCod"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ContratoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COD_CONTRATO")
    private Integer codContrato;

    @Column(name = "DESCRICAO", length = 500, nullable = false)
    private String descricao;

    @Column(name = "DATA_INICIO", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "DATA_FIM", nullable = false)
    private LocalDate dataFim;

    @Column(name = "TIPO_CONTRATO", length = 50, nullable = false)
    private String tipoContrato;

    @Column(name = "OBS", length = 500)
    private String obs;

    /** 1 = ativo, 2 = encerrado/inativo. */
    @Column(name = "STATUS", nullable = false)
    private Integer status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COD_PESSOA_FISICA")
    private PessoaFisicaEntity pessoaFisicaCod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COD_PESSOA_JURIDICA")
    private PessoaJuridicaEntity pessoaJuridicaCod;

}
