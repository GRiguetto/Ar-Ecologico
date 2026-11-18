package com.example.demo.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Confere a tradução das mensagens do SQL Server (PT e EN) para o usuário. */
class GlobalExceptionHandlerTest {

    @Test
    void cpfRepetido() {
        assertEquals("Já existe um cadastro com esse CPF.", GlobalExceptionHandler.traduzirErroDoBanco(
                "Violação da restrição UNIQUE KEY 'UQ__PESSOAS___C1F89731'. Não é possível inserir a chave duplicada "
                        + "no objeto 'dbo.PESSOAS_FISICAS'. O valor de chave duplicada é (12345678901)."));
    }

    @Test
    void excluirComRegistrosLigados() {
        assertEquals("Não é possível excluir: existem ordens de serviço ligados a este registro. "
                + "Exclua ou desvincule esses itens primeiro.", GlobalExceptionHandler.traduzirErroDoBanco(
                "The DELETE statement conflicted with the REFERENCE constraint \"FK__ORDENS_SE__PESSO__4E88ABD4\". "
                        + "The conflict occurred in database \"bd_ar\", table \"dbo.ORDENS_SERVICOS\", column 'PESSOA_F_COD'."));
    }

    @Test
    void descontoMaiorQueOTotal() {
        assertEquals("Valores inválidos no serviço realizado: a quantidade deve ser maior que zero, "
                + "o valor não pode ser negativo e o desconto não pode passar do total do item.",
                GlobalExceptionHandler.traduzirErroDoBanco(
                        "A instrução INSERT conflitou com a restrição do CHECK \"CK__SERVICOS___5AEE82B9\". "
                                + "O conflito ocorreu no banco de dados \"bd_ar\", tabela \"dbo.SERVICOS_REALIZADOS\"."));
    }

    @Test
    void textoGrandeDemais() {
        assertEquals("Um dos textos é maior do que o permitido. Encurte o texto e tente novamente.",
                GlobalExceptionHandler.traduzirErroDoBanco(
                        "Os dados de cadeia ou binários seriam truncados na tabela 'bd_ar.dbo.PESSOAS', coluna 'CEP'."));
    }

    @Test
    void erroDesconhecidoNaoETraduzido() {
        assertNull(GlobalExceptionHandler.traduzirErroDoBanco("Nome de objeto 'VW_QUALQUER' inválido."));
    }
}
