/* =====================================================================
   AR Ecologico - BACKUP E RESTORE DO BANCO bd_ar
   ---------------------------------------------------------------------
   BACKUP  = copia o banco inteiro (tabelas, dados, views, functions,
             procedures e usuarios) para um arquivo .bak.
   RESTORE = recria o banco a partir desse arquivo .bak.

   Onde fica o arquivo:
     Usamos so o nome 'bd_ar.bak' (sem pasta). Assim o SQL Server grava na
     pasta PADRAO de backup da instancia, onde ele ja tem permissao.
     (Com uma pasta propria, ex.: C:\Backup, e comum dar
      "Operating system error 5 - Acesso negado".)
     Para ver qual e a pasta, rode a PARTE 0.

   Rode no SSMS com um login administrador (ex.: autenticacao do Windows),
   NAO com o ar_app: ele nao tem permissao de backup/restore.
   Rode UMA PARTE POR VEZ (selecione o bloco e aperte F5).
   ===================================================================== */


/* ---------------------------------------------------------------------
   PARTE 0 - Pasta padrao de backup da instancia
   --------------------------------------------------------------------- */
SELECT SERVERPROPERTY('InstanceDefaultBackupPath') AS PASTA_BACKUP;
GO


/* ---------------------------------------------------------------------
   PARTE 1 - BACKUP
   FORMAT, INIT = sobrescreve o arquivo (fica so o backup mais recente).
   CHECKSUM     = grava um "verificador" para detectar arquivo corrompido.
   STATS = 10   = mostra o progresso de 10% em 10% na aba Mensagens.
   --------------------------------------------------------------------- */
BACKUP DATABASE bd_ar
TO DISK = 'bd_ar.bak'
WITH FORMAT, INIT, CHECKSUM, STATS = 10,
     NAME = 'bd_ar - backup completo';
GO


/* ---------------------------------------------------------------------
   PARTE 2 - CONFERIR O BACKUP (nao altera nada)
   --------------------------------------------------------------------- */
-- O arquivo esta integro e pode ser restaurado?
RESTORE VERIFYONLY FROM DISK = 'bd_ar.bak' WITH CHECKSUM;
GO

-- O que tem dentro do arquivo (nome, data, tamanho):
RESTORE HEADERONLY FROM DISK = 'bd_ar.bak';
GO

-- Historico de backups do bd_ar registrado pelo SQL Server:
SELECT TOP 5
       bs.database_name        AS BANCO,
       bs.backup_finish_date   AS TERMINOU_EM,
       CAST(bs.backup_size / 1024 / 1024 AS DECIMAL(10,2)) AS TAMANHO_MB,
       bmf.physical_device_name AS ARQUIVO
FROM msdb.dbo.backupset bs
INNER JOIN msdb.dbo.backupmediafamily bmf ON bmf.media_set_id = bs.media_set_id
WHERE bs.database_name = 'bd_ar'
ORDER BY bs.backup_finish_date DESC;
GO


/* ---------------------------------------------------------------------
   PARTE 3 - RESTORE
   ATENCAO: substitui o bd_ar atual pelo conteudo do backup.
   Tudo o que foi gravado DEPOIS do backup e perdido.

   Pare a aplicacao Java antes (ela mantem conexoes abertas no banco).
   SINGLE_USER ... ROLLBACK IMMEDIATE derruba quem ainda estiver conectado,
   senao o restore falha com "o banco esta em uso".
   --------------------------------------------------------------------- */
USE master;
GO

ALTER DATABASE bd_ar SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
GO

RESTORE DATABASE bd_ar
FROM DISK = 'bd_ar.bak'
WITH REPLACE, CHECKSUM, STATS = 10;
GO

ALTER DATABASE bd_ar SET MULTI_USER;
GO

-- Se o restore for feito em OUTRO computador/instancia, o usuario ar_app
-- do banco fica "orfao" (sem ligacao com o login). Para religar:
--   USE bd_ar;
--   ALTER USER ar_app WITH LOGIN = ar_app;
-- (o login ar_app precisa existir nessa instancia: veja CriarLogin.sql)


/* ---------------------------------------------------------------------
   PARTE 4 - ROTEIRO PARA DEMONSTRAR NA APRESENTACAO
   1) Rode a PARTE 1 (backup).
   2) Rode o bloco abaixo: cadastra um equipamento "de teste" e mostra.
   3) Rode a PARTE 3 (restore).
   4) Rode o SELECT de novo: o equipamento sumiu, porque o banco voltou
      ao estado do backup.
   (Outra opcao: apagar algo de proposito depois do backup e mostrar que
    o restore traz de volta.)
   --------------------------------------------------------------------- */
USE bd_ar;
GO

INSERT INTO EQUIPAMENTOS (BTUS, MODELO, MARCA, OBS)
VALUES (12000, 'TESTE BACKUP', 'TESTE', 'Inserido depois do backup');

SELECT COUNT(*) AS TOTAL_EQUIPAMENTOS FROM EQUIPAMENTOS;
SELECT * FROM EQUIPAMENTOS WHERE MODELO = 'TESTE BACKUP';
GO
