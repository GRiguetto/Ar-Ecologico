package com.example.demo.service;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ContratoVencendoDto;
import com.example.demo.dto.DashboardCartoesDto;

/**
 * Lê os números do dashboard direto das VIEWS e FUNCTIONS do banco
 * (sql/views_e_functions.sql). Aqui não há regra de negócio em Java:
 * o cálculo fica no banco, e o Java só consulta e devolve em JSON.
 *
 * Usa JdbcTemplate (SQL puro) porque views e functions não são entidades JPA.
 */
@Service
public class DashboardService {

    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** inicio/fim podem ser null: a function ignora o limite que vier vazio. */
    public DashboardCartoesDto cartoes(LocalDate inicio, LocalDate fim) {
        // 1) VIEW: uma linha com os 4 cards que não dependem de período.
        DashboardCartoesDto semPeriodo = jdbc.queryForObject(
                "SELECT TOTAL_CLIENTES, CONTRATOS_ATIVOS, OS_EM_ABERTO, CONTRATOS_VENCENDO_30_DIAS FROM VW_DASHBOARD_CARTOES",
                (linha, n) -> new DashboardCartoesDto(
                        linha.getInt("TOTAL_CLIENTES"),
                        linha.getInt("CONTRATOS_ATIVOS"),
                        linha.getInt("OS_EM_ABERTO"),
                        linha.getInt("CONTRATOS_VENCENDO_30_DIAS"),
                        null, null));

        // 2) FUNCTIONS escalares: recebem o período e devolvem um valor.
        Object[] periodo = { inicio, fim };
        int[] tipos = { Types.DATE, Types.DATE };
        BigDecimal faturamento = jdbc.queryForObject(
                "SELECT dbo.FN_FATURAMENTO_PERIODO(?, ?)", periodo, tipos, BigDecimal.class);
        BigDecimal ticketMedio = jdbc.queryForObject(
                "SELECT dbo.FC_ORDEM_SERVICO_MEDIA(?, ?)", periodo, tipos, BigDecimal.class);

        return new DashboardCartoesDto(semPeriodo.totalClientes(), semPeriodo.contratosAtivos(),
                semPeriodo.osEmAberto(), semPeriodo.contratosVencendo30Dias(), faturamento, ticketMedio);
    }

    /** VIEW que devolve várias linhas: cada linha vira um objeto da lista. */
    public List<ContratoVencendoDto> contratosVencendo() {
        return jdbc.query(
                "SELECT COD_CONTRATO, DESCRICAO, NOME_CLIENTE, DATA_FIM, DIAS_RESTANTES "
                        + "FROM VW_CONTRATOS_VENCENDO_30_DIAS ORDER BY DATA_FIM",
                (linha, n) -> new ContratoVencendoDto(
                        linha.getInt("COD_CONTRATO"),
                        linha.getString("DESCRICAO"),
                        linha.getString("NOME_CLIENTE"),
                        linha.getObject("DATA_FIM", LocalDate.class),
                        linha.getInt("DIAS_RESTANTES")));
    }
}
