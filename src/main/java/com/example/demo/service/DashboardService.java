package com.example.demo.service;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ContratoVencendoDto;
import com.example.demo.dto.DashboardCartoesDto;
import com.example.demo.dto.HistoricoServicoDto;
import com.example.demo.dto.OrdemServicoPessoaDto;
import com.example.demo.dto.ResumoPrestadorDto;

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

    /**
     * VIEW VW_ORDENS_SERVICO_PESSOAS: OS com nome/documento do cliente e o prestador.
     * inicio pode ser null (todo o período); senão filtra pela data de abertura.
     */
    public List<OrdemServicoPessoaDto> ordensServicoPessoas(LocalDate inicio) {
        return jdbc.query(
                "SELECT COD_OS, TIPO_PESSOA, NOME_CLIENTE, DOCUMENTO, TIPO_OS, STATUS_DESCRICAO, DATA_ABERTURA, "
                        + "VALOR_OS, DESCRICAO_PROBLEMA, O_QUE_FOI_FEITO, NOME_PRESTADOR "
                        + "FROM VW_ORDENS_SERVICO_PESSOAS "
                        + "WHERE (? IS NULL OR DATA_ABERTURA >= ?) "
                        + "ORDER BY DATA_ABERTURA DESC, COD_OS DESC",
                new Object[] { inicio, inicio }, new int[] { Types.DATE, Types.DATE },
                (linha, n) -> new OrdemServicoPessoaDto(
                        linha.getInt("COD_OS"),
                        linha.getString("TIPO_PESSOA"),
                        linha.getString("NOME_CLIENTE"),
                        linha.getString("DOCUMENTO"),
                        linha.getString("TIPO_OS"),
                        linha.getString("STATUS_DESCRICAO"),
                        linha.getObject("DATA_ABERTURA", LocalDate.class),
                        linha.getBigDecimal("VALOR_OS"),
                        linha.getString("DESCRICAO_PROBLEMA"),
                        linha.getString("O_QUE_FOI_FEITO"),
                        linha.getString("NOME_PRESTADOR")));
    }

    /**
     * FUNCTION QUE RETORNA TABELA (inline): FC_HISTORICO_SERVICOS(@NOME).
     * É usada como se fosse uma tabela, no FROM, passando o nome buscado.
     */
    public List<HistoricoServicoDto> historicoServicos(String nome) {
        return jdbc.query(
                "SELECT COD_OS, NOME, CIDADE, DOCUMENTO, TIPO_OS, STATUS_DESCRICAO, DATA_ABERTURA, DATA_EXECUCAO, "
                        + "VALOR, DESCRICAO_PROBLEMA, O_QUE_FOI_FEITO "
                        + "FROM dbo.FC_HISTORICO_SERVICOS(?) "
                        + "ORDER BY DATA_ABERTURA DESC, COD_OS DESC",
                new Object[] { nome }, new int[] { Types.VARCHAR },
                (linha, n) -> new HistoricoServicoDto(
                        linha.getInt("COD_OS"),
                        linha.getString("NOME"),
                        linha.getString("CIDADE"),
                        linha.getString("DOCUMENTO"),
                        linha.getString("TIPO_OS"),
                        linha.getString("STATUS_DESCRICAO"),
                        linha.getObject("DATA_ABERTURA", LocalDate.class),
                        linha.getObject("DATA_EXECUCAO", LocalDate.class),
                        linha.getBigDecimal("VALOR"),
                        linha.getString("DESCRICAO_PROBLEMA"),
                        linha.getString("O_QUE_FOI_FEITO")));
    }

    /**
     * FUNCTION QUE RETORNA VARIÁVEL TIPO TABELA (multi-statement):
     * FC_RESUMO_PRESTADORES(@inicio, @fim). No Java a chamada é igual à de
     * uma function de tabela; a diferença está no SQL (a function preenche a
     * variável @RESUMO com INSERT e depois faz RETURN).
     */
    public List<ResumoPrestadorDto> resumoPrestadores(LocalDate inicio, LocalDate fim) {
        return jdbc.query(
                "SELECT PRESTADOR_COD, NOME_PRESTADOR, QTD_CONCLUIDAS, FATURAMENTO "
                        + "FROM dbo.FC_RESUMO_PRESTADORES(?, ?) "
                        + "ORDER BY FATURAMENTO DESC",
                new Object[] { inicio, fim }, new int[] { Types.DATE, Types.DATE },
                (linha, n) -> new ResumoPrestadorDto(
                        linha.getInt("PRESTADOR_COD"),
                        linha.getString("NOME_PRESTADOR"),
                        linha.getInt("QTD_CONCLUIDAS"),
                        linha.getBigDecimal("FATURAMENTO")));
    }
}
