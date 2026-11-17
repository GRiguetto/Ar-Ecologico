package com.example.demo.database.procedure;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Executa as procedures de cadastro (sql/procedures.sql).
 *
 * Exemplo:
 *   Integer cod = procedures.executarRetornandoCodigo("sp_CadEquipamento",
 *           Procedures.inteiro("btus", 12000),
 *           Procedures.texto("modelo", "Split"),
 *           Procedures.texto("marca", "LG"),
 *           Procedures.texto("obs", null));
 *
 * vira o SQL:
 *   EXEC sp_CadEquipamento @btus = ?, @modelo = ?, @marca = ?, @obs = ?
 *
 * Os "?" recebem os valores com o tipo SQL informado (por isso NULL funciona).
 * Se a procedure der THROW (ex.: CPF repetido), o Spring transforma o erro em
 * exceção, e o GlobalExceptionHandler responde 409 para a tela.
 */
@Component
public class Procedures {

    /** Um parâmetro da procedure: nome (sem @), valor e tipo SQL (java.sql.Types). */
    public record Parametro(String nome, Object valor, int tipoSql) {
    }

    public static Parametro texto(String nome, String valor) {
        return new Parametro(nome, valor, Types.VARCHAR);
    }

    public static Parametro inteiro(String nome, Integer valor) {
        return new Parametro(nome, valor, Types.INTEGER);
    }

    public static Parametro decimal(String nome, BigDecimal valor) {
        return new Parametro(nome, valor, Types.DECIMAL);
    }

    public static Parametro data(String nome, LocalDate valor) {
        return new Parametro(nome, valor, Types.DATE);
    }

    public static Parametro booleano(String nome, Boolean valor) {
        return new Parametro(nome, valor, Types.BIT);
    }

    private final JdbcTemplate jdbc;

    public Procedures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Para procedures que terminam com SELECT do código gerado (COD_PESSOA, COD_CONTRATO...). */
    public Integer executarRetornandoCodigo(String procedure, Parametro... parametros) {
        return jdbc.queryForObject(montarSql(procedure, parametros), valores(parametros), tipos(parametros), Integer.class);
    }

    /** Para procedures que não devolvem nada (tabelas de chave composta). */
    public void executar(String procedure, Parametro... parametros) {
        jdbc.update(montarSql(procedure, parametros), valores(parametros), tipos(parametros));
    }

    private static String montarSql(String procedure, Parametro... parametros) {
        String lista = Arrays.stream(parametros)
                .map(p -> "@" + p.nome() + " = ?")
                .collect(Collectors.joining(", "));
        return "EXEC " + procedure + (lista.isEmpty() ? "" : " " + lista);
    }

    private static Object[] valores(Parametro... parametros) {
        return Arrays.stream(parametros).map(Parametro::valor).toArray();
    }

    private static int[] tipos(Parametro... parametros) {
        return Arrays.stream(parametros).mapToInt(Parametro::tipoSql).toArray();
    }
}
