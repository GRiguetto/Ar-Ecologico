package com.example.demo.handler;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.exception.ErrorResponse;
import com.example.demo.exception.NaoAutorizadoException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.RegraNegocioException;

/**
 * Transforma qualquer erro em uma resposta JSON { message, status } com
 * mensagem em português, sem detalhes técnicos (SQL, nome de constraint,
 * stack trace). O detalhe técnico vai para o LOG do servidor (console),
 * onde o desenvolvedor consegue investigar.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERRO_INESPERADO =
            "Ocorreu um erro inesperado no servidor. Tente novamente; se continuar, avise o administrador.";

    /** Nome da tabela no banco -> como o usuário entende. */
    private static final Map<String, String> TABELAS = Map.of(
            "ORDENS_SERVICOS", "ordens de serviço",
            "CONTRATOS", "contratos",
            "CONTRATOS_EQUIPAMENTOS", "equipamentos ligados a contratos",
            "SERVICOS_REALIZADOS", "serviços realizados em ordens de serviço",
            "PRESTADOR_SERVICOS", "cadastros de funcionário",
            "PESSOAS_FISICAS", "cadastros de pessoa física",
            "PESSOAS_JURIDICAS", "cadastros de pessoa jurídica");

    private static final Pattern TABELA_NA_MENSAGEM = Pattern.compile("(?:TABELA|TABLE) \"DBO\\.(\\w+)\"");

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(NaoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> handleNaoAutorizado(NaoAutorizadoException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErrorResponse> handleRegraNegocio(RegraNegocioException ex) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ClassCastException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        log.warn("Requisição inválida: {}", ex.getMessage());
        return resposta(HttpStatus.BAD_REQUEST, "Algum dado enviado é inválido. Confira os campos e tente novamente.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para '" + ex.getName() + "': " + ex.getValue());
    }

    /** Violação de regra do banco: UNIQUE (CPF/CNPJ), CHECK, FK, NOT NULL, texto grande demais. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detalhe = ex.getMostSpecificCause().getMessage();
        log.warn("Regra do banco violada: {}", detalhe);
        String mensagem = traduzirErroDoBanco(detalhe);
        return resposta(HttpStatus.CONFLICT, mensagem != null ? mensagem
                : "Os dados não puderam ser gravados porque violam uma regra do banco. Confira os campos e tente novamente.");
    }

    /** Outros erros de banco (ex.: THROW de procedure, view/function inexistente, banco fora do ar). */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        String detalhe = ex.getMostSpecificCause().getMessage();
        String mensagem = traduzirErroDoBanco(detalhe);
        if (mensagem != null) {
            log.warn("Regra do banco violada: {}", detalhe);
            return resposta(HttpStatus.CONFLICT, mensagem);
        }
        log.error("Erro de acesso ao banco", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Não foi possível acessar o banco de dados agora. Tente novamente; se continuar, avise o administrador.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Exceções do próprio Spring MVC (rota inexistente, método não suportado etc.)
        // já carregam o status HTTP correto.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            return resposta(springError.getStatusCode(), ex.getMessage());
        }
        log.error("Erro inesperado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, ERRO_INESPERADO);
    }

    /**
     * Lê a mensagem do SQL Server (em português ou inglês) e devolve uma frase
     * para o usuário. Devolve null quando não reconhece o tipo de erro.
     */
    static String traduzirErroDoBanco(String detalhe) {
        if (detalhe == null) {
            return null;
        }
        String d = detalhe.toUpperCase(Locale.ROOT);

        if (d.contains("UNIQUE") || d.contains("DUPLICATE") || d.contains("DUPLICADA") || d.contains("PRIMARY KEY")) {
            if (d.contains("PESSOAS_FISICAS")) return "Já existe um cadastro com esse CPF.";
            if (d.contains("PESSOAS_JURIDICAS")) return "Já existe um cadastro com esse CNPJ.";
            if (d.contains("CONTRATOS_EQUIPAMENTOS")) {
                return "Esse equipamento já está neste contrato. Edite a quantidade em vez de cadastrar de novo.";
            }
            if (d.contains("SERVICOS_REALIZADOS")) {
                return "Esse serviço já foi lançado nesta ordem de serviço. Edite o lançamento existente.";
            }
            return "Já existe um registro com esses dados.";
        }
        if (d.contains("FOREIGN KEY") || d.contains("REFERENCE")) {
            if (d.contains("DELETE")) {
                return "Não é possível excluir: existem " + tabelaLigada(d)
                        + " ligados a este registro. Exclua ou desvincule esses itens primeiro.";
            }
            return "Um dos itens selecionados não existe mais. Atualize a página e tente novamente.";
        }
        if (d.contains("CHECK")) {
            if (d.contains("SERVICOS_REALIZADOS")) {
                return "Valores inválidos no serviço realizado: a quantidade deve ser maior que zero, "
                        + "o valor não pode ser negativo e o desconto não pode passar do total do item.";
            }
            if (d.contains("CONTRATOS_EQUIPAMENTOS")) return "A quantidade de equipamentos deve ser maior que zero.";
            if (d.contains("ORDENS_SERVICOS")) return "Confira o status e o cliente da ordem de serviço.";
            if (d.contains("CONTRATOS")) return "Confira a situação e o cliente do contrato.";
            return "Algum valor informado não é permitido. Confira os campos e tente novamente.";
        }
        if (d.contains("TRUNCAT") || d.contains("TRUNCAD")) {   // "would be truncated" / "seriam truncados"
            return "Um dos textos é maior do que o permitido. Encurte o texto e tente novamente.";
        }
        if (d.contains("NULL")) {
            return "Preencha todos os campos obrigatórios.";
        }
        return null;
    }

    private static String tabelaLigada(String mensagemMaiuscula) {
        Matcher m = TABELA_NA_MENSAGEM.matcher(mensagemMaiuscula);
        return m.find() ? TABELAS.getOrDefault(m.group(1), "registros") : "registros";
    }

    private ResponseEntity<ErrorResponse> resposta(HttpStatusCode status, String mensagem) {
        ErrorResponse response = ErrorResponse.builder()
                .message(mensagem)
                .status(String.valueOf(status.value()))
                .build();
        return ResponseEntity.status(status).body(response);
    }

}
