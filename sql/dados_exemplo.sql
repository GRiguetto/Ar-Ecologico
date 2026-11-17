/* =====================================================================
   AR Ecologico - DADOS DE EXEMPLO (opcional)
   Rode DEPOIS de arEcologico.sql e sql\procedures.sql.
     sqlcmd -S localhost -E -C -x -f 65001 -d bd_ar -i sql\dados_exemplo.sql
   (-x evita que o sqlcmd tente interpretar o $ do hash da senha)
   ===================================================================== */

/* ------------------------------------------------------------------ */
/* 3. DADOS DE EXEMPLO                                                 */
/*    Correcao (doc. secao 8): SENHA e FORMA_PAGAMENTO sao informados. */
/*    Senha do prestador de exemplo = "123456" (hash BCrypt abaixo).   */
/* ------------------------------------------------------------------ */

EXEC sp_CadPessoaFisica
    @nome = 'Joao Silva', @endereco = 'Rua das Flores', @cep = '01001000',
    @numero = '100', @cidade = 'Sao Paulo', @estado = 'SP',
    @telefone = '11999998888', @cpf = '12345678901',
    @dataNascimento = '1990-05-20', @email = 'joao@email.com', @rg = '123456789';

EXEC sp_CadPessoaJuridica
    @nome = 'Climatiza Ar', @endereco = 'Av. Paulista', @cep = '01310100',
    @numero = '1500', @cidade = 'Sao Paulo', @estado = 'SP',
    @telefone = '1133334444', @cnpj = '12345678000199',
    @razaoSocial = 'Climatiza Ar Condicionado LTDA', @email = 'contato@climatiza.com';

EXEC sp_CadPrestadorServico
    @nome = 'Carlos Tecnico', @endereco = 'Rua B', @cep = '02002000',
    @numero = '45', @cidade = 'Sao Paulo', @estado = 'SP',
    @telefone = '11988887777', @cpf = '98765432100',
    @dataNascimento = '1985-03-10', @pagamento = 3500.00,
    @senha = '$2a$10$ROsNlEYn0gJXWRnhzpLBtuzQADwqZEQtp3szwcvsqNlqi.5yww4o.',
    @administrador = 0;

INSERT INTO EQUIPAMENTOS (BTUS, MODELO, MARCA, OBS) VALUES
    (9000,  'Split Hi-Wall Inverter', 'LG',      NULL),
    (12000, 'Split Hi-Wall',          'Samsung', 'Sala de reunioes'),
    (24000, 'Piso-Teto',              'Daikin',  NULL);

INSERT INTO SERVICOS (DATA_INICIO, DATA_FIM, VALOR_SERVICO, DESCRICAO) VALUES
    ('2026-01-01', '2026-12-31', 150.00, 'Limpeza completa'),
    ('2026-01-01', '2026-12-31',  80.00, 'Troca de filtro'),
    ('2026-01-01', '2026-12-31', 450.00, 'Instalacao');

INSERT INTO CONTRATOS (DESCRICAO, DATA_INICIO, DATA_FIM, TIPO_CONTRATO, OBS, STATUS, COD_PESSOA_FISICA, COD_PESSOA_JURIDICA) VALUES
    ('Manutencao preventiva residencial', '2026-01-01', '2026-12-31', 'Manutencao Mensal', NULL, 1, 1, NULL),
    ('Manutencao do escritorio',          '2026-03-01', '2027-02-28', 'Trimestral',        NULL, 1, NULL, 2);

INSERT INTO CONTRATOS_EQUIPAMENTOS (CONTRATO_COD, EQUIPAMENTO_COD, QTD) VALUES
    (1, 1, 2),
    (2, 2, 5),
    (2, 3, 1);

INSERT INTO ORDENS_SERVICOS (TIPO_OS, VALOR, OBS, DATA_ABERTURA, STATUS, DESCRICAO_PROBLEMA, DESCRICAO_EXECUTADO, DATA_EXECUCAO, PESSOA_F_COD, PESSOA_J_COD, CONTRATO_COD, PRESTADOR_SERVICO_COD) VALUES
    ('Preventiva', 300.00, NULL, '2026-06-02', 3, 'Limpeza semestral', 'Limpeza dos 2 aparelhos', '2026-06-05', 1, NULL, 1, 3),
    ('Corretiva',  NULL,   NULL, '2026-11-10', 1, 'Aparelho pingando agua', NULL, NULL, NULL, 2, 2, NULL);

INSERT INTO SERVICOS_REALIZADOS (OS_COD, SERVICO_COD, QTD, VALOR, DESCONTO, OBS, FORMA_PAGAMENTO) VALUES
    (1, 1, 2, 150.00, 0.00, NULL, 'PIX');
GO
