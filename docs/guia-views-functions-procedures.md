# Guia: views, functions e procedures, do banco até a tela

Este guia mostra como o AR Ecológico usa **views e functions** para os cards do dashboard e **procedures** para os cadastros, e o caminho que cada dado percorre até aparecer no navegador. Todos os exemplos são do código real do projeto.

## 1. O que é cada um

| Objeto | O que é | Recebe parâmetro? | Como se consulta | Uso no projeto |
|---|---|---|---|---|
| **View** | Um `SELECT` salvo com nome. Não guarda dados: roda o SELECT na hora. | Não | `SELECT * FROM VW_CONTRATOS_TOTAIS_ATIVOS` | Cards que não dependem de filtro |
| **Function escalar** | Recebe parâmetros e devolve **um valor** | Sim | `SELECT dbo.FN_FATURAMENTO_PERIODO('2026-01-01', '2026-12-31')` | Cards que dependem de período |
| **Function de tabela** | Recebe parâmetros e devolve **linhas** | Sim | `SELECT * FROM FC_HISTORICO_SERVICOS('Maria')` | Consultas e relatórios por nome |
| **Procedure** | Bloco de comandos que **altera dados** (INSERT, UPDATE...), com transação | Sim | `EXEC sp_CadPessoaFisica @nome = '...', ...` | Cadastros (POST) |

Regra prática:
- **Ler** dados com cálculo → view (sem filtro) ou function (com filtro).
- **Gravar** dados → procedure.

## 2. Arquivos

| Arquivo | Conteúdo |
|---|---|
| [arEcologico.sql](../arEcologico.sql) | 1º: banco e tabelas (script oficial do time) |
| [sql/procedures.sql](../sql/procedures.sql) | 2º: as 9 procedures de cadastro |
| [sql/views_e_functions.sql](../sql/views_e_functions.sql) | 3º: **todas** as views e functions. É o arquivo para estudar. |
| [sql/dados_exemplo.sql](../sql/dados_exemplo.sql) | 4º (opcional): dados de teste |

Todos usam `CREATE OR ALTER`: dá para rodar de novo depois de alterar algo, sem apagar nada antes.

```powershell
sqlcmd -S localhost -E -C -f 65001 -d teste1 -i sql\procedures.sql
sqlcmd -S localhost -E -C -f 65001 -d teste1 -i sql\views_e_functions.sql
```

Ou abra o arquivo no SSMS, escolha o banco `teste1` e aperte **F5**.

> **Permissões:** o login da aplicação (`ar_app`) precisa de `SELECT` para ler views (já tem, pelo `db_datareader`) e de `EXECUTE` para functions escalares e procedures (já tem, pelo `GRANT EXECUTE`). Criar ou alterar esses objetos exige um login com mais permissão. Por isso os scripts são rodados com `-E` (seu usuário do Windows).

## 3. Caminho de um card: do banco até a tela

Exemplo: os cards **"Contratos ativos"** e **"Faturamento no período"**.

```
 BANCO (SQL Server)                JAVA (Spring Boot)                       FRONT (navegador)
 ─────────────────                 ──────────────────                       ─────────────────
 VW_CONTRATOS_TOTAIS_ATIVOS ─┐
 VW_PESSOAS_TOTAIS ──────────┤
 VW_OS_POR_STATUS ───────────┼─► VW_DASHBOARD_CARTOES ─┐
 VW_CONTRATOS_VENCENDO_30 ───┘                         │
                                                       ├─► DashboardService ─► DashboardController ─► GET /dashboard/cartoes
 FN_FATURAMENTO_PERIODO(ini, fim) ─────────────────────┤      (JdbcTemplate)                              │  JSON
 FC_ORDEM_SERVICO_MEDIA(ini, fim) ─────────────────────┘                                                  ▼
                                                                                             script.js: Repositorio.dashboardCartoes()
                                                                                                        montarDashboard() desenha os cards
```

### Passo 1: a view no banco ([sql/views_e_functions.sql](../sql/views_e_functions.sql))

```sql
CREATE OR ALTER VIEW VW_CONTRATOS_TOTAIS_ATIVOS
AS
SELECT COUNT(C.COD_CONTRATO) AS TOTAL_CONTRATOS
FROM CONTRATOS C
WHERE C.STATUS = 1;
```

A `VW_DASHBOARD_CARTOES` junta 4 views numa linha só. Assim o Java faz **uma** consulta para os 4 cards que não dependem de período:

```sql
CREATE OR ALTER VIEW VW_DASHBOARD_CARTOES
AS
SELECT PT.TOTAL_CLIENTES, CA.TOTAL_CONTRATOS AS CONTRATOS_ATIVOS, ...
FROM VW_PESSOAS_TOTAIS PT
CROSS JOIN VW_CONTRATOS_TOTAIS_ATIVOS CA
CROSS JOIN VW_OS_POR_STATUS ST;
```

Teste no SSMS: `SELECT * FROM VW_DASHBOARD_CARTOES;`

### Passo 2: o Java consulta a view e as functions ([DashboardService.java](../src/main/java/com/example/demo/service/DashboardService.java))

Views e functions **não são entidades JPA**, então o Java usa SQL direto com o `JdbcTemplate`:

```java
// VIEW: uma linha -> um objeto
DashboardCartoesDto semPeriodo = jdbc.queryForObject(
        "SELECT TOTAL_CLIENTES, CONTRATOS_ATIVOS, OS_EM_ABERTO, CONTRATOS_VENCENDO_30_DIAS FROM VW_DASHBOARD_CARTOES",
        (linha, n) -> new DashboardCartoesDto(linha.getInt("TOTAL_CLIENTES"), linha.getInt("CONTRATOS_ATIVOS"), ...));

// FUNCTION: os "?" recebem o período
BigDecimal faturamento = jdbc.queryForObject(
        "SELECT dbo.FN_FATURAMENTO_PERIODO(?, ?)", periodo, tipos, BigDecimal.class);
```

- `queryForObject`: quando o SELECT devolve **uma** linha (cards).
- `query`: quando devolve **várias** linhas. Veja `contratosVencendo()` no mesmo arquivo, que lê a `VW_CONTRATOS_VENCENDO_30_DIAS`.

### Passo 3: o controller expõe uma URL ([DashboardController.java](../src/main/java/com/example/demo/controller/DashboardController.java))

```java
@GetMapping("/cartoes")
public DashboardCartoesDto cartoes(@RequestParam(required = false) LocalDate inicio,
                                   @RequestParam(required = false) LocalDate fim) {
    return service.cartoes(inicio, fim);
}
```

Teste no Postman: **GET** `http://localhost:8080/dashboard/cartoes?inicio=2026-01-01&fim=2026-12-31`

```json
{ "totalClientes": 6, "contratosAtivos": 5, "osEmAberto": 3,
  "contratosVencendo30Dias": 1, "faturamento": 600.00, "ticketMedio": 600.00 }
```

Os nomes do JSON vêm dos campos do record [DashboardCartoesDto.java](../src/main/java/com/example/demo/dto/DashboardCartoesDto.java).

### Passo 4: o front busca e desenha ([script.js](../src/main/resources/static/js/script.js))

```js
// Repositorio: chama a URL
dashboardCartoes(inicio, fim) {
  return requisitar(`/dashboard/cartoes?inicio=${inicio}&fim=${fim}`);
},

// montarDashboard(): usa os campos do JSON
['Contratos ativos', cartoes.contratosAtivos, 'Contrato.html'],
['Faturamento no período', formatarMoeda(cartoes.faturamento)],
```

Para ver isso acontecendo: abra o dashboard, aperte **F12 → aba Rede (Network)**, troque o período e clique na chamada `cartoes`. Você vê a URL e o JSON que veio do banco.

### Tabela completa dos cards

| Card | Objeto no banco | Campo no JSON |
|---|---|---|
| Clientes | `VW_PESSOAS_TOTAIS.TOTAL_CLIENTES` | `totalClientes` |
| Contratos ativos | `VW_CONTRATOS_TOTAIS_ATIVOS.TOTAL_CONTRATOS` | `contratosAtivos` |
| OS em aberto | `VW_OS_POR_STATUS` (ABERTAS + EM_ANDAMENTO) | `osEmAberto` |
| Contratos vencendo em 30 dias | `VW_CONTRATOS_VENCENDO_30_DIAS` (contagem) | `contratosVencendo30Dias` |
| Faturamento no período | `FN_FATURAMENTO_PERIODO(@inicio, @fim)` | `faturamento` |
| Ticket médio por OS | `FC_ORDEM_SERVICO_MEDIA(@inicio, @fim)` | `ticketMedio` |

## 4. Caminho de um cadastro: da tela até a procedure

Exemplo: cadastrar um cliente pessoa física.

```
 FRONT                                   JAVA                                                BANCO
 ─────                                   ────                                                ─────
 Formulário "Clientes" → Salvar
 script.js: API.clientes.salvar()
   POST /PessoasFisicas  (JSON) ─────► PessoaFisicaController
                                         └► PessoaFisicaService.salvaPessoa()
                                              └► procedures.executarRetornandoCodigo(
                                                   "sp_CadPessoaFisica", texto("nome", ...), ...) ─► EXEC sp_CadPessoaFisica @nome = ?, ...
                                                                                                    INSERT PESSOAS
                                                                                                    INSERT PESSOAS_FISICAS
                                                                                                    COMMIT
                                              ◄──────────────────────────── COD_PESSOA = 25 ◄────── SELECT @idPessoa
                                              └► repository.findById(25)
   ◄──────────────── JSON do cliente salvo
 tabela atualiza
```

### O Java chamando a procedure ([PessoaFisicaService.java](../src/main/java/com/example/demo/service/PessoaFisicaService.java))

```java
Integer codPessoa = procedures.executarRetornandoCodigo("sp_CadPessoaFisica",
        texto("nome", dados.getNome()),
        texto("cpf", dados.getCpf()),
        data("dataNascimento", dados.getDataNascimento()),
        texto("rg", dados.getRg()),          // pode ser null: vira NULL na procedure
        ...);

return repository.findById(codPessoa).orElseThrow();
```

O componente [Procedures.java](../src/main/java/com/example/demo/database/procedure/Procedures.java) monta o comando `EXEC sp_CadPessoaFisica @nome = ?, @cpf = ?, ...` e envia cada valor com o seu tipo SQL. Por isso valores nulos (RG, e-mail) funcionam.

### As 9 procedures e quem as chama

| Procedure | Tabelas | Service Java | Devolve |
|---|---|---|---|
| `sp_CadPessoaFisica` | PESSOAS + PESSOAS_FISICAS | `PessoaFisicaService` | `COD_PESSOA` |
| `sp_CadPessoaJuridica` | PESSOAS + PESSOAS_JURIDICAS | `PessoaJuridicaService` | `COD_PESSOA` |
| `sp_CadPrestadorServico` | PESSOAS + PESSOAS_FISICAS + PRESTADOR_SERVICOS | `PrestadorServicoService` | `COD_PESSOA` |
| `sp_CadContrato` | CONTRATOS | `ContratoService` | `COD_CONTRATO` |
| `sp_CadEquipamento` | EQUIPAMENTOS | `EquipamentoService` | `COD_EQUIPAMENTO` |
| `sp_CadContratoEquipamento` | CONTRATOS_EQUIPAMENTOS | `ContratoEquipamentoService` | nada (chave composta) |
| `sp_CadOrdemServico` | ORDENS_SERVICOS | `OrdemServicoService` | `COD_OS` |
| `sp_CadServico` | SERVICOS | `ServicoService` | `COD_SERVICO` |
| `sp_CadServicoRealizado` | SERVICOS_REALIZADOS | `ServicoRealizadoService` | nada (chave composta) |

**Editar e excluir** (PUT, PATCH, DELETE) continuam pelo JPA, porque a documentação do banco só define procedures de **cadastro**. Se o time criar procedures de alteração (`sp_AtualizaContrato`, por exemplo), elas entram no mesmo modelo: o service troca o `repository.save(...)` por `procedures.executar(...)`.

## 5. O que mudou nas 3 procedures do time, e por quê

A lógica é a mesma: cadastrar a pessoa, pegar o código com `SCOPE_IDENTITY()` e cadastrar a tabela filha na mesma transação. Mudaram 4 pontos, necessários para o **sistema** usar a procedure (no SSMS, a versão antiga parecia funcionar):

**Antes (versão do time):**
```sql
begin tran
    begin try
        insert ...
        commit
        print 'Dados da pessoa fisica cadastrados com sucesso!'
    end try
    begin catch
        rollback
        print 'Dados da pessoa fisica não cadastrados! Erro: ' + ERROR_MESSAGE()
    end catch
```

**Depois:**
```sql
SET NOCOUNT ON;                    -- (1)
BEGIN TRY
    BEGIN TRAN;
        INSERT ...
    COMMIT;
    SELECT @idPessoa AS COD_PESSOA;  -- (4)
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;     -- (3)
    THROW;                           -- (2)
END CATCH
```

| # | Mudança | Por quê |
|---|---|---|
| 1 | `SET NOCOUNT ON` | Sem isso, o SQL Server manda a mensagem "(1 linha afetada)" antes do resultado, e o Java pode ler essa mensagem em vez do código gerado. |
| 2 | `THROW` em vez de `PRINT` | **O mais importante.** Com `PRINT`, um erro (CPF repetido, por exemplo) vira só um texto e a procedure termina "com sucesso". O sistema acharia que cadastrou e a tela não mostraria erro nenhum. Com `THROW`, o erro chega ao Java, que responde 409 e a tela mostra "Já existe um cadastro com esse CPF/CNPJ". |
| 3 | `IF @@TRANCOUNT > 0 ROLLBACK` e `BEGIN TRAN` dentro do `TRY` | Só desfaz se ainda houver transação aberta (ponto 1 da seção 8 da documentação do banco). |
| 4 | `SELECT` do código no final | O sistema precisa do código gerado para buscar o registro e devolver o JSON para a tela. |

Além disso:
- **Senha:** o Java gera o **hash BCrypt** antes de chamar `sp_CadPrestadorServico`. A procedure nunca recebe a senha em texto. No exemplo do script do time (`@senha = 'minhaSenha123'`), esse usuário **não conseguiria fazer login**, porque o login compara com um hash.
- **Acentos:** o arquivo `Procedures_AR.sql` mostra `transa��o` porque foi salvo em ANSI e lido como UTF-8. Salve os `.sql` como **UTF-8** (no VS Code: canto inferior direito → *Save with Encoding* → UTF-8).

## 6. Como criar um card novo (roteiro)

Exemplo: card **"OS concluídas"**.

1. **Banco.** A informação já existe na `VW_OS_POR_STATUS` (coluna `CONCLUIDAS`). Acrescente-a à `VW_DASHBOARD_CARTOES` em [sql/views_e_functions.sql](../sql/views_e_functions.sql):
   ```sql
   ST.CONCLUIDAS AS OS_CONCLUIDAS,
   ```
   Rode o script e teste: `SELECT * FROM VW_DASHBOARD_CARTOES;`
2. **DTO.** Acrescente `Integer osConcluidas` ao record `DashboardCartoesDto`.
3. **Service.** Inclua `OS_CONCLUIDAS` no SELECT e `linha.getInt("OS_CONCLUIDAS")` no `DashboardService`.
4. **Postman.** `GET /dashboard/cartoes` deve mostrar `"osConcluidas": ...`.
5. **Front.** Acrescente a linha `['OS concluídas', cartoes.osConcluidas, 'Servico.html'],` na lista de cards do `montarDashboard()`.

Se o card depender de um filtro (período, cliente...), crie uma **function** em vez de uma coluna na view, e passe o filtro como parâmetro na URL, como o faturamento faz com `inicio` e `fim`.

## 7. Próximos passos possíveis

Os **gráficos** do dashboard ainda são calculados no navegador. As views para migrá-los já existem no arquivo de estudo e seguem o mesmo caminho da seção 3 (view → `jdbc.query` → endpoint → `script.js`):

| Gráfico | View pronta |
|---|---|
| OS por status | `VW_OS_POR_STATUS` |
| OS por tipo | `VW_OS_POR_TIPO` |
| Pagamento por funcionário | `VW_PAGAMENTO_PRESTADOR_SERVICO` |
| Equipamentos por contrato | `VW_CONTRATOS_PESSOAS_EQUIPAMENTOS` |
| Últimas ordens de serviço | `VW_ORDENS_SERVICO_PESSOAS` |

Para busca por nome (tela de histórico, por exemplo) existem `FC_HISTORICO_SERVICOS('nome')` e `FC_CONTRATOS_CLIENTES('nome')`.
