/* =====================================================================
   AR Ecologico - VIEWS E FUNCTIONS
   ---------------------------------------------------------------------
   VIEW      = um SELECT salvo com nome. Voce consulta como se fosse tabela:
               SELECT * FROM VW_CONTRATOS_TOTAIS_ATIVOS
               Nao recebe parametro e nao guarda dados (roda o SELECT na hora).

   FUNCTION  = recebe parametros.
     - Escalar: devolve UM valor.       SELECT dbo.FN_FATURAMENTO_PERIODO('2026-01-01', '2026-12-31')
     - Tabela:  devolve linhas/colunas. SELECT * FROM FC_HISTORICO_SERVICOS('Maria')

   Regra pratica: o card nao depende de filtro -> VIEW.
                  o card depende de periodo/nome/codigo -> FUNCTION.

   Base: views/functions do time (banco BD_A) e a "Documentacao do Banco
   de Dados - bd_ar". Correcoes da secao 8 da documentacao ja aplicadas:
     - CREATE OR ALTER (o script pode rodar varias vezes; sem view duplicada)
     - GO antes de cada CREATE (CREATE VIEW/FUNCTION precisa iniciar o lote)
     - status 4 padronizado como 'CANCELADA'
     - toda coluna calculada com apelido (AS ...)
     - NENHUM DROP DATABASE no arquivo

   Como executar:
     sqlcmd -S localhost -E -C -f 65001 -d bd_ar -i sql\views_e_functions.sql
   ou abra no SSMS e execute (F5). O USE abaixo garante o banco certo.
   ===================================================================== */

USE bd_ar;
GO


/* =====================================================================
   PARTE A - CARDS DO DASHBOARD
   ---------------------------------------------------------------------
   Card no front                  Objeto no banco
   -----------------------------  ------------------------------------------
   Clientes                       VW_PESSOAS_TOTAIS.TOTAL_CLIENTES
   Contratos ativos               VW_CONTRATOS_TOTAIS_ATIVOS.TOTAL_CONTRATOS
   OS em aberto                   VW_OS_POR_STATUS (ABERTAS + EM_ANDAMENTO)
   Contratos vencendo em 30 dias  VW_CONTRATOS_VENCENDO_30_DIAS (contagem)
   Faturamento no periodo         FN_FATURAMENTO_PERIODO(@inicio, @fim)
   Ticket medio por OS            FC_ORDEM_SERVICO_MEDIA(@inicio, @fim)

   VW_DASHBOARD_CARTOES junta as 4 views numa linha so: o sistema faz
   UMA consulta para os 4 cards que nao dependem de periodo.
   ===================================================================== */

-- Totais de clientes. CORRECAO: a versao original contava os prestadores
-- como pessoa fisica (a doc avisa). Aqui prestador NAO conta como cliente.
CREATE OR ALTER VIEW VW_PESSOAS_TOTAIS
AS
SELECT
    T.PF                AS TOTAL_PESSOAS_FISICAS,
    T.PJ                AS TOTAL_PESSOAS_JURIDICAS,
    T.PF + T.PJ         AS TOTAL_CLIENTES,
    T.PRESTADORES       AS TOTAL_PRESTADORES
FROM (
    SELECT
        (SELECT COUNT(*) FROM PESSOAS_FISICAS PF
          WHERE NOT EXISTS (SELECT 1 FROM PRESTADOR_SERVICOS PS
                             WHERE PS.PRESTADOR_COD = PF.PESSOA_FISICA_COD))   AS PF,
        (SELECT COUNT(*) FROM PESSOAS_JURIDICAS)                             AS PJ,
        (SELECT COUNT(*) FROM PRESTADOR_SERVICOS)                            AS PRESTADORES
) T;
GO

-- Quantidade de contratos ativos (STATUS = 1).
CREATE OR ALTER VIEW VW_CONTRATOS_TOTAIS_ATIVOS
AS
SELECT COUNT(C.COD_CONTRATO) AS TOTAL_CONTRATOS
FROM CONTRATOS C
WHERE C.STATUS = 1;
GO

-- Resumo de OS por situacao (versao com COUNT unica).
CREATE OR ALTER VIEW VW_OS_POR_STATUS
AS
SELECT
    COUNT(*)                                        AS TOTAL_ORDEM_SERVICO,
    SUM(CASE WHEN STATUS = 1 THEN 1 ELSE 0 END)     AS ABERTAS,
    SUM(CASE WHEN STATUS = 2 THEN 1 ELSE 0 END)     AS EM_ANDAMENTO,
    SUM(CASE WHEN STATUS = 3 THEN 1 ELSE 0 END)     AS CONCLUIDAS,
    SUM(CASE WHEN STATUS = 4 THEN 1 ELSE 0 END)     AS CANCELADAS
FROM ORDENS_SERVICOS;
GO

-- Contratos ativos que terminam nos proximos 30 dias (lista).
CREATE VIEW VW_CONTRATOS_VENCENDO_30_DIAS
AS
SELECT
    C.COD_CONTRATO,
    C.DESCRICAO,
    P.NOME                                                  AS NOME_CLIENTE,
    C.DATA_FIM,
    DATEDIFF(DAY, CAST(GETDATE() AS DATE), C.DATA_FIM)      AS DIAS_RESTANTES
FROM CONTRATOS C
INNER JOIN PESSOAS P ON P.COD_PESSOA = COALESCE(C.COD_PESSOA_FISICA, C.COD_PESSOA_JURIDICA)
WHERE C.STATUS = 1
  AND C.DATA_FIM BETWEEN CAST(GETDATE() AS DATE) AND DATEADD(DAY, 30, CAST(GETDATE() AS DATE));
GO

-- Os 4 cards "sem periodo" numa linha so (view usando outras views).
CREATE OR ALTER VIEW VW_DASHBOARD_CARTOES
AS
SELECT
    PT.TOTAL_CLIENTES                                           AS TOTAL_CLIENTES,
    CA.TOTAL_CONTRATOS                                          AS CONTRATOS_ATIVOS,
    ISNULL(ST.ABERTAS, 0) + ISNULL(ST.EM_ANDAMENTO, 0)          AS OS_EM_ABERTO,
    (SELECT COUNT(*) FROM VW_CONTRATOS_VENCENDO_30_DIAS)        AS CONTRATOS_VENCENDO_30_DIAS
FROM VW_PESSOAS_TOTAIS PT
CROSS JOIN VW_CONTRATOS_TOTAIS_ATIVOS CA
CROSS JOIN VW_OS_POR_STATUS ST;
GO

-- Faturamento: soma de VALOR das OS concluidas (STATUS = 3) no periodo.
-- Data considerada: DATA_EXECUCAO ou, se vazia, DATA_ABERTURA.
-- Parametro NULL = sem limite naquele lado.
CREATE OR ALTER FUNCTION FN_FATURAMENTO_PERIODO (@DataInicio DATE, @DataFim DATE)
RETURNS DECIMAL(12,2)
AS
BEGIN
    DECLARE @total DECIMAL(12,2);

    SELECT @total = ISNULL(SUM(VALOR), 0)
    FROM   ORDENS_SERVICOS
    WHERE  STATUS = 3
      AND  (@DataInicio IS NULL OR COALESCE(DATA_EXECUCAO, DATA_ABERTURA) >= @DataInicio)
      AND  (@DataFim    IS NULL OR COALESCE(DATA_EXECUCAO, DATA_ABERTURA) <= @DataFim);

    RETURN @total;
END
GO

-- Ticket medio: media de VALOR das OS concluidas no periodo (mesmo filtro).
CREATE OR ALTER FUNCTION FC_ORDEM_SERVICO_MEDIA (@DataInicio DATE, @DataFim DATE)
RETURNS DECIMAL(12,2)
AS
BEGIN
    DECLARE @media DECIMAL(12,2);

    SELECT @media = ISNULL(CAST(AVG(VALOR) AS DECIMAL(12,2)), 0)
    FROM   ORDENS_SERVICOS
    WHERE  STATUS = 3
      AND  (@DataInicio IS NULL OR COALESCE(DATA_EXECUCAO, DATA_ABERTURA) >= @DataInicio)
      AND  (@DataFim    IS NULL OR COALESCE(DATA_EXECUCAO, DATA_ABERTURA) <= @DataFim);

    RETURN @media;
END
GO


/* =====================================================================
   PARTE B - DEMAIS VIEWS E FUNCTIONS DA DOCUMENTACAO
   (prontas para virar graficos/relatorios no mesmo caminho dos cards)
   ===================================================================== */

-- Contratos ativos com o cliente e a quantidade de equipamentos.
CREATE OR ALTER VIEW VW_CONTRATOS_PESSOAS_EQUIPAMENTOS
AS
SELECT
    C.COD_CONTRATO,
    CASE WHEN C.COD_PESSOA_FISICA IS NOT NULL THEN 'FISICA' ELSE 'JURIDICA' END AS TIPO_PESSOA,
    P.COD_PESSOA,
    P.NOME,
    COALESCE(PF.CPF, PJ.CNPJ)       AS DOCUMENTO,
    PJ.RAZAO_SOCIAL,
    C.TIPO_CONTRATO,
    C.DESCRICAO                     AS DESCRICAO_CONTRATO,
    C.DATA_INICIO,
    C.DATA_FIM,
    COUNT(CE.EQUIPAMENTO_COD)       AS QTD_TIPOS_EQUIPAMENTOS,
    COALESCE(SUM(CE.QTD), 0)        AS QTD_TOTAL_EQUIPAMENTOS
FROM CONTRATOS C
INNER JOIN PESSOAS P               ON P.COD_PESSOA = COALESCE(C.COD_PESSOA_FISICA, C.COD_PESSOA_JURIDICA)
LEFT  JOIN PESSOAS_FISICAS PF      ON PF.PESSOA_FISICA_COD   = C.COD_PESSOA_FISICA
LEFT  JOIN PESSOAS_JURIDICAS PJ    ON PJ.PESSOA_JURIDICA_COD = C.COD_PESSOA_JURIDICA
LEFT  JOIN CONTRATOS_EQUIPAMENTOS CE ON CE.CONTRATO_COD = C.COD_CONTRATO
WHERE C.STATUS = 1
GROUP BY C.COD_CONTRATO, C.COD_PESSOA_FISICA, P.COD_PESSOA, P.NOME, PF.CPF, PJ.CNPJ,
         PJ.RAZAO_SOCIAL, C.TIPO_CONTRATO, C.DESCRICAO, C.DATA_INICIO, C.DATA_FIM;
GO

-- Ordens de servico com cliente e prestador responsavel.
CREATE OR ALTER VIEW VW_ORDENS_SERVICO_PESSOAS
AS
SELECT
    OS.COD_OS,
    CASE WHEN OS.PESSOA_F_COD IS NOT NULL THEN 'FISICA' ELSE 'JURIDICA' END AS TIPO_PESSOA,
    P.COD_PESSOA,
    P.NOME                          AS NOME_CLIENTE,
    COALESCE(PF.CPF, PJ.CNPJ)       AS DOCUMENTO,
    OS.TIPO_OS,
    CASE OS.STATUS
        WHEN 1 THEN 'ABERTA'
        WHEN 2 THEN 'EM ANDAMENTO'
        WHEN 3 THEN 'CONCLUIDA'
        WHEN 4 THEN 'CANCELADA'
    END                             AS STATUS_DESCRICAO,
    OS.DATA_ABERTURA,
    OS.VALOR                        AS VALOR_OS,
    OS.DESCRICAO_PROBLEMA,
    OS.DESCRICAO_EXECUTADO          AS O_QUE_FOI_FEITO,
    OS.PRESTADOR_SERVICO_COD        AS PRESTADOR,
    PP.NOME                         AS NOME_PRESTADOR
FROM ORDENS_SERVICOS OS
INNER JOIN PESSOAS P             ON P.COD_PESSOA = COALESCE(OS.PESSOA_F_COD, OS.PESSOA_J_COD)
LEFT  JOIN PESSOAS_FISICAS PF    ON PF.PESSOA_FISICA_COD   = OS.PESSOA_F_COD
LEFT  JOIN PESSOAS_JURIDICAS PJ  ON PJ.PESSOA_JURIDICA_COD = OS.PESSOA_J_COD
LEFT  JOIN PRESTADOR_SERVICOS PS ON PS.PRESTADOR_COD = OS.PRESTADOR_SERVICO_COD
LEFT  JOIN PESSOAS PP            ON PP.COD_PESSOA    = PS.PRESTADOR_COD;
GO

-- Cada OS com os servicos lancados (uma linha por servico).
CREATE OR ALTER VIEW VW_OS_SERVICOS_REALIZADOS
AS
SELECT
    OS.COD_OS,
    OS.TIPO_OS,
    CASE OS.STATUS
        WHEN 1 THEN 'ABERTA'
        WHEN 2 THEN 'EM ANDAMENTO'
        WHEN 3 THEN 'CONCLUIDA'
        WHEN 4 THEN 'CANCELADA'
    END                             AS STATUS_DESCRICAO,
    OS.DATA_ABERTURA                AS [DATA DE ABERTURA],
    OS.DATA_EXECUCAO                AS [DATA DE EXECUÇÃO],
    OS.DESCRICAO_PROBLEMA           AS [O QUE ACONTECEU],
    OS.DESCRICAO_EXECUTADO          AS [O QUE FOI FEITO],
    S.COD_SERVICO,
    S.DESCRICAO,
    SR.QTD,
    SR.VALOR,
    SR.DESCONTO,
    SR.FORMA_PAGAMENTO,
    SR.OBS                          AS OBS_SERVICO
FROM ORDENS_SERVICOS OS
INNER JOIN SERVICOS_REALIZADOS SR ON SR.OS_COD = OS.COD_OS
INNER JOIN SERVICOS S             ON S.COD_SERVICO = SR.SERVICO_COD;
GO

-- OS com STATUS = 1 (abertas). CORRECAO: faltava o apelido da coluna.
CREATE OR ALTER VIEW VW_ORDEM_SERVICO_TOTAIS_ATIVOS
AS
SELECT COUNT(OS.COD_OS) AS TOTAL_ORDEM_SERVICO
FROM ORDENS_SERVICOS OS
WHERE OS.STATUS = 1;
GO

-- Quantidade de OS por tipo (grafico "OS por tipo").
CREATE OR ALTER VIEW VW_OS_POR_TIPO
AS
SELECT TIPO_OS, COUNT(COD_OS) AS QTD
FROM ORDENS_SERVICOS
GROUP BY TIPO_OS;
GO

-- Nome e pagamento de cada prestador (grafico "Pagamento por funcionario").
CREATE OR ALTER VIEW VW_PAGAMENTO_PRESTADOR_SERVICO
AS
SELECT
    PS.PRESTADOR_COD    AS COD_PRESTADOR,
    P.NOME              AS NOME_PRESTADOR,
    PS.PAGAMENTO
FROM PRESTADOR_SERVICOS PS
INNER JOIN PESSOAS P ON P.COD_PESSOA = PS.PRESTADOR_COD;
GO

-- Total liquido de UMA OS: soma de (QTD * VALOR - DESCONTO). 0 se nao houver itens.
CREATE OR ALTER FUNCTION FC_TOTAL_OS (@COD_OS INT)
RETURNS DECIMAL(12,2)
AS
BEGIN
    RETURN (SELECT ISNULL(SUM(QTD * VALOR - DESCONTO), 0)
            FROM SERVICOS_REALIZADOS
            WHERE OS_COD = @COD_OS);
END
GO

-- Historico de OS de clientes cujo nome contenha o texto (busca parcial).
CREATE OR ALTER FUNCTION FC_HISTORICO_SERVICOS (@NOME VARCHAR(100))
RETURNS TABLE
AS
RETURN
(
    SELECT  P.NOME, P.CIDADE, P.TELEFONE, PF.CPF, PJ.CNPJ,
            COALESCE(PF.CPF, PJ.CNPJ) AS DOCUMENTO,
            OS.COD_OS,
            OS.TIPO_OS,
            CASE OS.STATUS
                WHEN 1 THEN 'ABERTA'
                WHEN 2 THEN 'EM ANDAMENTO'
                WHEN 3 THEN 'CONCLUIDA'
                WHEN 4 THEN 'CANCELADA'
            END                     AS STATUS_DESCRICAO,
            OS.DATA_ABERTURA,
            OS.DATA_EXECUCAO,
            OS.VALOR,
            OS.DESCRICAO_PROBLEMA,
            OS.DESCRICAO_EXECUTADO  AS O_QUE_FOI_FEITO
    FROM PESSOAS P
    LEFT  JOIN PESSOAS_FISICAS   PF ON P.COD_PESSOA = PF.PESSOA_FISICA_COD
    LEFT  JOIN PESSOAS_JURIDICAS PJ ON P.COD_PESSOA = PJ.PESSOA_JURIDICA_COD
    INNER JOIN ORDENS_SERVICOS   OS ON OS.PESSOA_F_COD = P.COD_PESSOA OR OS.PESSOA_J_COD = P.COD_PESSOA
    WHERE P.NOME LIKE '%' + @NOME + '%'
);
GO

-- Contratos + equipamentos + dados do cliente, buscando pelo nome.
-- Uma linha por equipamento; contrato sem equipamento vem com campos vazios.
CREATE OR ALTER FUNCTION FC_CONTRATOS_CLIENTES (@NOME VARCHAR(100))
RETURNS TABLE
AS
RETURN
(
    SELECT
        C.COD_CONTRATO, C.TIPO_CONTRATO, C.DESCRICAO AS DESCRICAO_CONTRATO,
        C.DATA_INICIO, C.DATA_FIM, C.OBS AS OBS_CONTRATO,
        P.NOME, P.ENDERECO, P.CEP, P.CIDADE, P.ESTADO, P.TELEFONE, P.EMAIL,
        CASE WHEN C.COD_PESSOA_FISICA IS NOT NULL THEN 'FISICA' ELSE 'JURIDICA' END AS TIPO_PESSOA,
        PF.CPF, PF.RG, PF.DATA_NASCIMENTO, PJ.CNPJ, PJ.RAZAO_SOCIAL,
        E.COD_EQUIPAMENTO, E.MARCA, E.MODELO, E.BTUS, CE.QTD AS QTD_EQUIPAMENTO
    FROM CONTRATOS C
    INNER JOIN PESSOAS P                 ON P.COD_PESSOA = COALESCE(C.COD_PESSOA_FISICA, C.COD_PESSOA_JURIDICA)
    LEFT  JOIN PESSOAS_FISICAS PF        ON PF.PESSOA_FISICA_COD   = C.COD_PESSOA_FISICA
    LEFT  JOIN PESSOAS_JURIDICAS PJ      ON PJ.PESSOA_JURIDICA_COD = C.COD_PESSOA_JURIDICA
    LEFT  JOIN CONTRATOS_EQUIPAMENTOS CE ON CE.CONTRATO_COD = C.COD_CONTRATO
    LEFT  JOIN EQUIPAMENTOS E            ON E.COD_EQUIPAMENTO = CE.EQUIPAMENTO_COD
    WHERE P.NOME LIKE '%' + @NOME + '%'
);
GO

-- Faturamento por prestador no periodo.
-- FUNCTION QUE RETORNA VARIAVEL TIPO TABELA:
--   1) RETURNS @RESUMO TABLE (...)  -> declara a variavel tipo tabela
--   2) INSERT INTO @RESUMO ...      -> preenche a variavel
--   3) RETURN                       -> devolve a variavel
-- So conta OS concluidas (STATUS = 3), pela data de execucao.
-- Usada no sistema: GET /dashboard/resumo-prestadores (tabela "Resumo por prestador").
CREATE FUNCTION FC_RESUMO_PRESTADORES (@DataInicio DATE, @DataFim DATE)
RETURNS @RESUMO TABLE (
    PRESTADOR_COD   INT,
    NOME_PRESTADOR  VARCHAR(100),
    QTD_CONCLUIDAS  INT,
    FATURAMENTO     DECIMAL(12,2)
)
AS
BEGIN
    INSERT INTO @RESUMO
    SELECT  OS.PRESTADOR_SERVICO_COD,
            P.NOME,
            COUNT(*),
            SUM(OS.VALOR)
    FROM ORDENS_SERVICOS OS
    INNER JOIN PESSOAS P ON P.COD_PESSOA = OS.PRESTADOR_SERVICO_COD
    WHERE OS.STATUS = 3
      AND (@DataInicio IS NULL OR OS.DATA_EXECUCAO >= @DataInicio)
      AND (@DataFim    IS NULL OR OS.DATA_EXECUCAO <= @DataFim)
    GROUP BY OS.PRESTADOR_SERVICO_COD, P.NOME;

    RETURN;
END
GO


/* =====================================================================
   PARTE C - COMO CONSULTAR (rode no SSMS para ver o resultado)
   ---------------------------------------------------------------------
-- Cards (o que o endpoint GET /dashboard/cartoes executa):
SELECT * FROM VW_DASHBOARD_CARTOES;
SELECT dbo.FN_FATURAMENTO_PERIODO('2026-01-01', '2026-12-31') AS FATURAMENTO;
SELECT dbo.FC_ORDEM_SERVICO_MEDIA('2026-01-01', '2026-12-31') AS TICKET_MEDIO;
SELECT dbo.FN_FATURAMENTO_PERIODO(NULL, NULL) AS FATURAMENTO_TOTAL;   -- sem periodo

-- Cada view separada:
SELECT * FROM VW_PESSOAS_TOTAIS;
SELECT * FROM VW_CONTRATOS_TOTAIS_ATIVOS;
SELECT * FROM VW_OS_POR_STATUS;
SELECT * FROM VW_CONTRATOS_VENCENDO_30_DIAS ORDER BY DATA_FIM;
SELECT * FROM VW_CONTRATOS_PESSOAS_EQUIPAMENTOS;
SELECT * FROM VW_ORDENS_SERVICO_PESSOAS WHERE STATUS_DESCRICAO = 'ABERTA';
SELECT * FROM VW_OS_SERVICOS_REALIZADOS;
SELECT * FROM VW_ORDEM_SERVICO_TOTAIS_ATIVOS;
SELECT * FROM VW_OS_POR_TIPO ORDER BY QTD DESC;
SELECT * FROM VW_PAGAMENTO_PRESTADOR_SERVICO;

-- Functions:
SELECT dbo.FC_TOTAL_OS(1) AS TOTAL_LIQUIDO_OS_1;
SELECT * FROM FC_HISTORICO_SERVICOS('Maria');
SELECT * FROM FC_CONTRATOS_CLIENTES('Clinica');
SELECT * FROM FC_RESUMO_PRESTADORES('2026-06-01', '2026-11-30') ORDER BY FATURAMENTO DESC;
SELECT * FROM FC_RESUMO_PRESTADORES(NULL, NULL);   -- todo o periodo

-- Conferir a regra "valor digitado x valor dos itens" (ponto 6 da secao 8 da doc):
SELECT COD_OS, VALOR AS VALOR_DIGITADO, dbo.FC_TOTAL_OS(COD_OS) AS VALOR_DOS_ITENS
FROM ORDENS_SERVICOS WHERE STATUS = 3;
   ===================================================================== */
