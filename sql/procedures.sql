/* =====================================================================
   AR Ecologico - PROCEDURES DE CADASTRO
   ---------------------------------------------------------------------
   O sistema (Java) faz TODO cadastro novo (POST) chamando estas procedures.

   Base: as 3 procedures do time (sp_CadPessoaFisica, sp_CadPessoaJuridica,
   sp_CadPrestadorServico). A logica e a mesma; mudaram 4 pontos para que o
   sistema consiga usa-las:

   1) SET NOCOUNT ON
      Sem isso o SQL Server devolve mensagens "(1 linha afetada)" antes do
      resultado, e o Java pode ler essa mensagem em vez do codigo gerado.

   2) THROW no CATCH (em vez de PRINT)
      Com PRINT, o erro vira so uma mensagem de texto e a procedure termina
      "com sucesso": o sistema acharia que cadastrou. THROW repassa o erro
      (ex.: CPF repetido) para o Java, que responde 409 para a tela.

   3) IF @@TRANCOUNT > 0 ROLLBACK
      So desfaz se ainda houver transacao aberta (ponto 1 da secao 8 da doc).
      E o BEGIN TRAN fica DENTRO do TRY.

   4) SELECT do codigo gerado no final
      O sistema precisa saber o codigo (COD_PESSOA, COD_CONTRATO...) para
      buscar o registro e devolver o JSON para a tela.

   Senha do prestador: o Java ja manda o HASH (BCrypt). Nunca o texto puro.

   Como executar (cria ou atualiza; pode rodar de novo sem problema):
     sqlcmd -S localhost -E -C -f 65001 -d bd_ar -i sql\procedures.sql
   ou abra no SSMS e execute (F5). O USE abaixo garante o banco certo.
   ===================================================================== */

USE bd_ar;
GO


-- 1) CADASTRO DE PESSOA FISICA  (PESSOAS -> PESSOAS_FISICAS)
CREATE OR ALTER PROCEDURE sp_CadPessoaFisica
    @nome            VARCHAR(100),
    @endereco        VARCHAR(50),
    @cep             CHAR(8),
    @numero          VARCHAR(10),
    @cidade          VARCHAR(50),
    @estado          CHAR(2),
    @telefone        VARCHAR(15),
    @cpf             CHAR(11),
    @dataNascimento  DATE,
    @email           VARCHAR(150) = NULL,
    @rg              CHAR(15)     = NULL
AS
BEGIN
    SET NOCOUNT ON;                                   -- (1)
    BEGIN TRY
        BEGIN TRAN;

            -- cadastrar os dados da pessoa
            INSERT INTO PESSOAS (ENDERECO, CEP, NUMERO, CIDADE, ESTADO, TELEFONE, NOME, EMAIL)
            VALUES (@endereco, @cep, @numero, @cidade, @estado, @telefone, @nome, @email);

            -- recupera o ultimo id gerado neste escopo
            DECLARE @idPessoa INT = SCOPE_IDENTITY();

            -- cadastrar os dados da pessoa fisica (mesmo codigo)
            INSERT INTO PESSOAS_FISICAS (PESSOA_FISICA_COD, CPF, RG, DATA_NASCIMENTO)
            VALUES (@idPessoa, @cpf, @rg, @dataNascimento);

        COMMIT;
        SELECT @idPessoa AS COD_PESSOA;               -- (4)
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;                  -- (3)
        THROW;                                        -- (2)
    END CATCH
END
GO


-- 2) CADASTRO DE PESSOA JURIDICA  (PESSOAS -> PESSOAS_JURIDICAS)
CREATE OR ALTER PROCEDURE sp_CadPessoaJuridica
    @nome         VARCHAR(100),
    @endereco     VARCHAR(50),
    @cep          CHAR(8),
    @numero       VARCHAR(10),
    @cidade       VARCHAR(50),
    @estado       CHAR(2),
    @telefone     VARCHAR(15),
    @cnpj         CHAR(14),
    @razaoSocial  VARCHAR(100),
    @email        VARCHAR(150) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRAN;

            INSERT INTO PESSOAS (ENDERECO, CEP, NUMERO, CIDADE, ESTADO, TELEFONE, NOME, EMAIL)
            VALUES (@endereco, @cep, @numero, @cidade, @estado, @telefone, @nome, @email);

            DECLARE @idPessoa INT = SCOPE_IDENTITY();

            INSERT INTO PESSOAS_JURIDICAS (PESSOA_JURIDICA_COD, CNPJ, RAZAO_SOCIAL)
            VALUES (@idPessoa, @cnpj, @razaoSocial);

        COMMIT;
        SELECT @idPessoa AS COD_PESSOA;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH
END
GO


-- 3) CADASTRO DE PRESTADOR DE SERVICOS  (PESSOAS -> PESSOAS_FISICAS -> PRESTADOR_SERVICOS)
CREATE OR ALTER PROCEDURE sp_CadPrestadorServico
    @nome            VARCHAR(100),
    @endereco        VARCHAR(50),
    @cep             CHAR(8),
    @numero          VARCHAR(10),
    @cidade          VARCHAR(50),
    @estado          CHAR(2),
    @telefone        VARCHAR(15),
    @cpf             CHAR(11),
    @dataNascimento  DATE,
    @pagamento       DECIMAL(10,2),
    @senha           VARCHAR(255),          -- HASH da senha (o Java gera)
    @administrador   BIT          = 1,
    @email           VARCHAR(150) = NULL,
    @rg              CHAR(15)     = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        BEGIN TRAN;

            -- 1o: dados da pessoa
            INSERT INTO PESSOAS (ENDERECO, CEP, NUMERO, CIDADE, ESTADO, TELEFONE, NOME, EMAIL)
            VALUES (@endereco, @cep, @numero, @cidade, @estado, @telefone, @nome, @email);

            DECLARE @idPessoa INT = SCOPE_IDENTITY();

            -- 2o: como pessoa fisica (mesmo codigo)
            INSERT INTO PESSOAS_FISICAS (PESSOA_FISICA_COD, CPF, RG, DATA_NASCIMENTO)
            VALUES (@idPessoa, @cpf, @rg, @dataNascimento);

            -- 3o: como prestador de servicos (mesmo codigo)
            INSERT INTO PRESTADOR_SERVICOS (PRESTADOR_COD, PAGAMENTO, ADMINISTRADOR, SENHA)
            VALUES (@idPessoa, @pagamento, @administrador, @senha);

        COMMIT;
        SELECT @idPessoa AS COD_PESSOA;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK;
        THROW;
    END CATCH
END
GO


/* ---------------------------------------------------------------------
   Os cadastros abaixo gravam em UMA tabela so. Um INSERT sozinho ja e
   atomico (ou grava tudo ou nada), entao nao precisam de BEGIN TRAN.
   Mantem o TRY/CATCH + THROW para o erro chegar ao sistema.
   --------------------------------------------------------------------- */

-- 4) CADASTRO DE CONTRATO  (informe PF OU PJ; o CHECK da tabela garante)
CREATE OR ALTER PROCEDURE sp_CadContrato
    @descricao          VARCHAR(500),
    @dataInicio         DATE,
    @dataFim            DATE,
    @tipoContrato       VARCHAR(50),
    @status             INT          = 1,      -- 1 ativo, 2 inativo
    @obs                VARCHAR(500) = NULL,
    @codPessoaFisica    INT          = NULL,
    @codPessoaJuridica  INT          = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO CONTRATOS (DESCRICAO, DATA_INICIO, DATA_FIM, TIPO_CONTRATO, OBS, STATUS,
                               COD_PESSOA_FISICA, COD_PESSOA_JURIDICA)
        VALUES (@descricao, @dataInicio, @dataFim, @tipoContrato, @obs, @status,
                @codPessoaFisica, @codPessoaJuridica);

        SELECT CAST(SCOPE_IDENTITY() AS INT) AS COD_CONTRATO;
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END
GO


-- 5) CADASTRO DE EQUIPAMENTO
CREATE OR ALTER PROCEDURE sp_CadEquipamento
    @btus    INT,
    @modelo  VARCHAR(100),
    @marca   VARCHAR(50),
    @obs     VARCHAR(300) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO EQUIPAMENTOS (BTUS, MODELO, MARCA, OBS)
        VALUES (@btus, @modelo, @marca, @obs);

        SELECT CAST(SCOPE_IDENTITY() AS INT) AS COD_EQUIPAMENTO;
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END
GO


-- 6) EQUIPAMENTO NO CONTRATO  (chave composta: nao gera codigo novo)
CREATE OR ALTER PROCEDURE sp_CadContratoEquipamento
    @contratoCod     INT,
    @equipamentoCod  INT,
    @qtd             INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO CONTRATOS_EQUIPAMENTOS (CONTRATO_COD, EQUIPAMENTO_COD, QTD)
        VALUES (@contratoCod, @equipamentoCod, @qtd);
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END
GO


-- 7) ABERTURA DE ORDEM DE SERVICO  (informe PF OU PJ; contrato e prestador opcionais)
CREATE OR ALTER PROCEDURE sp_CadOrdemServico
    @tipoOs               VARCHAR(50),
    @dataAbertura         DATE,
    @descricaoProblema    VARCHAR(500),
    @status               INT           = 1,   -- 1 aberta, 2 em andamento, 3 concluida, 4 cancelada
    @valor                DECIMAL(10,2) = NULL,
    @obs                  VARCHAR(500)  = NULL,
    @descricaoExecutado   VARCHAR(500)  = NULL,
    @dataExecucao         DATE          = NULL,
    @pessoaFCod           INT           = NULL,
    @pessoaJCod           INT           = NULL,
    @contratoCod          INT           = NULL,
    @prestadorServicoCod  INT           = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO ORDENS_SERVICOS (TIPO_OS, VALOR, OBS, DATA_ABERTURA, STATUS, DESCRICAO_PROBLEMA,
                                     DESCRICAO_EXECUTADO, DATA_EXECUCAO, PESSOA_F_COD, PESSOA_J_COD,
                                     CONTRATO_COD, PRESTADOR_SERVICO_COD)
        VALUES (@tipoOs, @valor, @obs, @dataAbertura, @status, @descricaoProblema,
                @descricaoExecutado, @dataExecucao, @pessoaFCod, @pessoaJCod,
                @contratoCod, @prestadorServicoCod);

        SELECT CAST(SCOPE_IDENTITY() AS INT) AS COD_OS;
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END
GO


-- 8) CADASTRO DE SERVICO (catalogo)
CREATE OR ALTER PROCEDURE sp_CadServico
    @dataInicio    DATE,
    @dataFim       DATE,
    @valorServico  DECIMAL(10,2),
    @descricao     VARCHAR(500)
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO SERVICOS (DATA_INICIO, DATA_FIM, VALOR_SERVICO, DESCRICAO)
        VALUES (@dataInicio, @dataFim, @valorServico, @descricao);

        SELECT CAST(SCOPE_IDENTITY() AS INT) AS COD_SERVICO;
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END
GO


-- 9) SERVICO REALIZADO EM UMA OS  (chave composta: nao gera codigo novo)
CREATE OR ALTER PROCEDURE sp_CadServicoRealizado
    @osCod           INT,
    @servicoCod      INT,
    @qtd             INT,
    @valor           DECIMAL(10,2),
    @formaPagamento  VARCHAR(50),
    @desconto        DECIMAL(10,2) = 0,
    @obs             VARCHAR(500)  = NULL
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRY
        INSERT INTO SERVICOS_REALIZADOS (OS_COD, SERVICO_COD, QTD, VALOR, DESCONTO, OBS, FORMA_PAGAMENTO)
        VALUES (@osCod, @servicoCod, @qtd, @valor, @desconto, @obs, @formaPagamento);
    END TRY
    BEGIN CATCH
        THROW;
    END CATCH
END
GO


/* ---------------------------------------------------------------------
   EXEMPLOS (rode um de cada vez no SSMS para testar sem o sistema)
   ---------------------------------------------------------------------
EXEC sp_CadPessoaFisica
    @nome = 'Joao Silva', @endereco = 'Rua das Flores', @cep = '01001000',
    @numero = '100', @cidade = 'Sao Paulo', @estado = 'SP',
    @telefone = '11999998888', @cpf = '12345678901',
    @dataNascimento = '1990-05-20', @email = 'joao@email.com', @rg = '123456789';
    -- devolve: COD_PESSOA = <codigo gerado>

EXEC sp_CadContrato
    @descricao = 'Manutencao mensal', @dataInicio = '2026-01-01', @dataFim = '2026-12-31',
    @tipoContrato = 'Anual', @codPessoaFisica = 1;

-- Erro de proposito: CPF repetido. Com THROW aparece a mensagem de erro (vermelha);
-- com o PRINT antigo apareceria so um texto e o sistema acharia que deu certo.
EXEC sp_CadPessoaFisica
    @nome = 'Outro', @endereco = 'Rua X', @cep = '01001000', @numero = '1',
    @cidade = 'Sao Paulo', @estado = 'SP', @telefone = '1', @cpf = '12345678901',
    @dataNascimento = '1990-01-01';
   --------------------------------------------------------------------- */
