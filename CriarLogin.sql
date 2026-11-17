--crair um login 
--           nome                   senha
CREATE LOGIN ar_app WITH PASSWORD = 'dba';
GO
--  nome banco
USE bd_ar;
GO

CREATE USER ar_app FOR LOGIN ar_app;
GO

ALTER ROLE db_datareader ADD MEMBER ar_app;
ALTER ROLE db_datawriter ADD MEMBER ar_app;
GO


--permição
--nome do banco
USE bd_ar;

GO
                              --nome do usuario criado
GRANT EXECUTE ON SCHEMA::dbo TO ar_app;
GO
