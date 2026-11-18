// =====================================================================
// AR Ecológico - script.js
// As telas trabalham com registros "de tela" (id, clienteId, status em texto...).
// A camada API (abaixo) traduz esses registros de/para o JSON real do back-end
// (codPessoa, pessoaFisicaCod, status 1..4, chaves compostas etc.).
// O endereço da API fica em config.js.
// =====================================================================

// ---------- Configuração das entidades (campos = colunas do banco) ----------
// Extras de cada campo:
//   obrigatorio      -> não pode ficar vazio (marcado com * na tela)
//   formato          -> máscara e/ou validação (cpf, cnpj, cep, telefone, email, nascimento, senha)
//   minimo           -> menor número aceito (campos numéricos)
// Regras que envolvem mais de um campo ficam em "regras" da entidade.
const campo = (nome, rotulo, tipo = 'text', extras = {}) => ({ nome, rotulo, tipo, ...extras });

const STATUS_OS = ['ABERTA', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA'];       // banco: 1, 2, 3, 4
const STATUS_CONTRATO = ['ATIVO', 'INATIVO'];                                  // banco: 1, 2
const ESTADOS = ['AC', 'AL', 'AM', 'AP', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT', 'PA', 'PB',
  'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO'];

// Listas iguais aos valores gravados no banco (sem acento, como nos scripts da pasta sql).
// O texto bonito que aparece na tela vem de ROTULOS_OPCOES (mais abaixo).
// Valores antigos que não estão na lista continuam aparecendo ao editar.
const TIPOS_OS = ['Manutencao Preventiva', 'Manutencao Corretiva', 'Instalacao', 'Limpeza', 'Recarga de Gas'];
const TIPOS_CONTRATO = ['Manutencao Preventiva', 'Manutencao Corretiva', 'Instalacao', 'Locacao de Equipamentos', 'PMOC'];
const FORMAS_PAGAMENTO = ['PIX', 'Cartao de Credito', 'Cartao de Debito', 'Dinheiro', 'Boleto', 'Transferencia'];

const dataFimAntesDoInicio = (r) =>
  (r.dataInicio && r.dataFim && r.dataFim < r.dataInicio
    ? { dataFim: 'A data de fim não pode ser anterior à data de início.' } : {});

// Campos de endereço/contato comuns a PESSOAS (todos NOT NULL, menos e-mail).
const camposDePessoa = () => [
  campo('telefone', 'Telefone (com DDD)', 'text', { obrigatorio: true, formato: 'telefone', atributos: 'maxlength="15" inputmode="tel" placeholder="(11) 91234-5678"' }),
  campo('cep', 'CEP', 'text', { obrigatorio: true, formato: 'cep', atributos: 'maxlength="9" inputmode="numeric" placeholder="00000-000"' }),
  campo('endereco', 'Endereço', 'text', { obrigatorio: true, atributos: 'maxlength="50"' }),
  campo('numero', 'Número', 'text', { obrigatorio: true, atributos: 'maxlength="10"' }),
  campo('cidade', 'Cidade', 'text', { obrigatorio: true, atributos: 'maxlength="50"' }),
  campo('estado', 'Estado (UF)', 'select', { opcoes: ESTADOS, obrigatorio: true }),
];

const ENTIDADES = {
  clientes: {
    titulo: 'Clientes', recurso: 'clientes', campoRotulo: 'nome',
    colunas: ['nome', 'tipoCliente', 'telefone', 'cidade'],
    campos: [
      campo('tipoCliente', 'Tipo de cliente', 'select', { opcoes: ['FISICA', 'JURIDICA'], obrigatorio: true, fixoNaEdicao: true }),
      campo('nome', 'Nome / Nome fantasia', 'text', { obrigatorio: true, atributos: 'maxlength="100"' }),
      campo('cpf', 'CPF', 'text', { obrigatorio: true, formato: 'cpf', somenteSe: ['tipoCliente', 'FISICA'], atributos: 'maxlength="14" inputmode="numeric" placeholder="000.000.000-00"' }),
      campo('rg', 'RG', 'text', { somenteSe: ['tipoCliente', 'FISICA'], atributos: 'maxlength="15"' }),
      campo('dataNascimento', 'Data de nascimento', 'date', { obrigatorio: true, formato: 'nascimento', somenteSe: ['tipoCliente', 'FISICA'] }),
      campo('cnpj', 'CNPJ', 'text', { obrigatorio: true, formato: 'cnpj', somenteSe: ['tipoCliente', 'JURIDICA'], atributos: 'maxlength="18" inputmode="numeric" placeholder="00.000.000/0000-00"' }),
      campo('razaoSocial', 'Razão social', 'text', { obrigatorio: true, somenteSe: ['tipoCliente', 'JURIDICA'], atributos: 'maxlength="100"' }),
      campo('email', 'E-mail', 'email', { formato: 'email', atributos: 'maxlength="150" placeholder="nome@empresa.com"' }),
      ...camposDePessoa(),
    ],
  },
  funcionarios: {
    titulo: 'Funcionários (Prestadores de serviço)', recurso: 'funcionarios', campoRotulo: 'nome',
    colunas: ['nome', 'email', 'administrador', 'pagamento'],
    campos: [
      campo('nome', 'Nome', 'text', { obrigatorio: true, atributos: 'maxlength="100"' }),
      campo('email', 'E-mail (usado no login)', 'email', { obrigatorio: true, formato: 'email', atributos: 'maxlength="150" placeholder="nome@empresa.com"' }),
      campo('senha', 'Senha (em branco = manter a atual)', 'password', { obrigatorioAoCriar: true, formato: 'senha', atributos: 'autocomplete="new-password"' }),
      campo('administrador', 'Administrador?', 'select', { opcoes: ['NAO', 'SIM'], obrigatorio: true }),
      campo('pagamento', 'Pagamento (R$)', 'number', { moeda: true, obrigatorio: true, minimo: 0, atributos: 'min="0"' }),
      campo('cpf', 'CPF', 'text', { obrigatorio: true, formato: 'cpf', atributos: 'maxlength="14" inputmode="numeric" placeholder="000.000.000-00"' }),
      campo('rg', 'RG', 'text', { atributos: 'maxlength="15"' }),
      campo('dataNascimento', 'Data de nascimento', 'date', { obrigatorio: true, formato: 'nascimento' }),
      ...camposDePessoa(),
    ],
  },
  contratos: {
    titulo: 'Contratos', recurso: 'contratos', campoRotulo: 'descricao',
    colunas: ['descricao', 'clienteId', 'tipoContrato', 'status', 'dataInicio', 'dataFim'],
    campos: [
      campo('clienteId', 'Cliente', 'select', { origem: 'clientes', obrigatorio: true }),
      campo('descricao', 'Descrição', 'text', { obrigatorio: true, atributos: 'maxlength="500"' }),
      campo('tipoContrato', 'Tipo de contrato', 'select', { opcoes: TIPOS_CONTRATO, obrigatorio: true }),
      campo('status', 'Situação', 'select', { opcoes: STATUS_CONTRATO, obrigatorio: true }),
      campo('dataInicio', 'Data de início', 'date', { obrigatorio: true }),
      campo('dataFim', 'Data de fim', 'date', { obrigatorio: true }),
      campo('obs', 'Observações', 'textarea', { atributos: 'maxlength="500"' }),
    ],
    regras: dataFimAntesDoInicio,
  },
  equipamentos: {
    titulo: 'Equipamentos (catálogo)', recurso: 'equipamentos',
    rotular: (e) => `${e.marca} ${e.modelo} (${e.btus} BTUs)`,
    colunas: ['marca', 'modelo', 'btus'],
    campos: [
      campo('marca', 'Marca', 'text', { obrigatorio: true, atributos: 'maxlength="50"' }),
      campo('modelo', 'Modelo', 'text', { obrigatorio: true, atributos: 'maxlength="100"' }),
      campo('btus', 'BTUs', 'number', { obrigatorio: true, minimo: 1, inteiro: true, atributos: 'min="1" step="1"' }),
      campo('obs', 'Observações', 'textarea', { atributos: 'maxlength="300"' }),
    ],
  },
  contratosEquipamentos: {
    titulo: 'Equipamentos de cada contrato', recurso: 'contratosEquipamentos',
    colunas: ['contratoId', 'equipamentoId', 'qtd'],
    campos: [
      campo('contratoId', 'Contrato', 'select', { origem: 'contratos', obrigatorio: true, fixoNaEdicao: true }),
      campo('equipamentoId', 'Equipamento', 'select', { origem: 'equipamentos', obrigatorio: true, fixoNaEdicao: true }),
      campo('qtd', 'Quantidade', 'number', { obrigatorio: true, minimo: 1, inteiro: true, atributos: 'min="1" step="1"' }),
    ],
  },
  ordensServico: {
    titulo: 'Ordens de Serviço', recurso: 'ordensServico',
    rotular: (o) => `OS ${o.id} - ${o.descricaoProblema}`,
    colunas: ['id', 'descricaoProblema', 'clienteId', 'funcionarioId', 'status', 'valor', 'dataAbertura'],
    campos: [
      campo('id', 'Nº', 'hidden'),
      campo('clienteId', 'Cliente', 'select', { origem: 'clientes', obrigatorio: true }),
      campo('contratoId', 'Contrato (opcional)', 'select', { origem: 'contratos' }),
      campo('funcionarioId', 'Funcionário responsável', 'select', { origem: 'funcionarios' }),
      campo('tipoOs', 'Tipo da OS', 'select', { opcoes: TIPOS_OS, obrigatorio: true }),
      campo('status', 'Status', 'select', { opcoes: STATUS_OS, obrigatorio: true }),
      campo('valor', 'Valor (R$)', 'number', { moeda: true, minimo: 0, atributos: 'min="0"' }),
      campo('dataAbertura', 'Data de abertura', 'date', { obrigatorio: true }),
      campo('dataExecucao', 'Data de execução', 'date', { dica: 'Obrigatória quando a OS estiver concluída.' }),
      campo('descricaoProblema', 'Descrição do problema', 'textarea', { obrigatorio: true, atributos: 'maxlength="500"' }),
      campo('descricaoExecutado', 'Descrição do executado', 'textarea', { dica: 'Obrigatória quando a OS estiver concluída.', atributos: 'maxlength="500"' }),
      campo('obs', 'Observações', 'textarea', { atributos: 'maxlength="500"' }),
    ],
    regras: (r) => {
      const erros = {};
      if (r.dataAbertura && r.dataExecucao && r.dataExecucao < r.dataAbertura) {
        erros.dataExecucao = 'A data de execução não pode ser anterior à data de abertura.';
      }
      if (r.status === 'CONCLUIDA') {
        if (!r.dataExecucao) erros.dataExecucao = 'Para concluir a OS, informe a data de execução.';
        if (r.valor === undefined) erros.valor = 'Para concluir a OS, informe o valor.';
        if (!r.descricaoExecutado) erros.descricaoExecutado = 'Para concluir a OS, descreva o que foi feito.';
      }
      return erros;
    },
  },
  servicosRealizados: {
    titulo: 'Serviços realizados em cada OS', recurso: 'servicosRealizados',
    colunas: ['osId', 'servicoId', 'qtd', 'valor', 'desconto', 'formaPagamento'],
    campos: [
      campo('osId', 'Ordem de serviço', 'select', { origem: 'ordensServico', obrigatorio: true, fixoNaEdicao: true }),
      campo('servicoId', 'Serviço', 'select', { origem: 'servicos', obrigatorio: true, fixoNaEdicao: true }),
      campo('qtd', 'Quantidade', 'number', { obrigatorio: true, minimo: 1, inteiro: true, atributos: 'min="1" step="1"' }),
      campo('valor', 'Valor unitário (R$)', 'number', { moeda: true, obrigatorio: true, minimo: 0, atributos: 'min="0"' }),
      campo('desconto', 'Desconto (R$)', 'number', { moeda: true, minimo: 0, atributos: 'min="0"' }),
      campo('formaPagamento', 'Forma de pagamento', 'select', { opcoes: FORMAS_PAGAMENTO, obrigatorio: true }),
      campo('obs', 'Observações', 'textarea', { atributos: 'maxlength="500"' }),
    ],
    regras: (r) => {
      const total = (r.qtd ?? 0) * (r.valor ?? 0);
      return r.qtd !== undefined && r.valor !== undefined && (r.desconto ?? 0) > total + 0.001
        ? { desconto: `O desconto não pode ser maior que o total do item (${formatarMoeda(total)}).` } : {};
    },
  },
  servicos: {
    titulo: 'Serviços (catálogo)', recurso: 'servicos', campoRotulo: 'descricao',
    colunas: ['descricao', 'valorServico', 'dataInicio', 'dataFim'],
    campos: [
      campo('descricao', 'Descrição', 'text', { obrigatorio: true, atributos: 'maxlength="500"' }),
      campo('valorServico', 'Valor do serviço (R$)', 'number', { moeda: true, obrigatorio: true, minimo: 0, atributos: 'min="0"' }),
      campo('dataInicio', 'Data de início', 'date', { obrigatorio: true }),
      campo('dataFim', 'Data de fim', 'date', { obrigatorio: true }),
    ],
    regras: dataFimAntesDoInicio,
  },
};

const rotuloDe = (entidade, registro) =>
  (entidade.rotular ? entidade.rotular(registro) : registro[entidade.campoRotulo]);

const CAMPOS_RESTRITOS_PARA_FUNCIONARIOS = {
  funcionarios: ['pagamento'],
  ordensServico: ['valor'],
  servicos: ['valorServico'],
  servicosRealizados: ['valor', 'desconto'],
};

// ---------- Comunicação com o back-end ----------
// O back-end já devolve mensagens em português (GlobalExceptionHandler).
// Aqui só cobrimos respostas sem texto útil (ex.: erro de proxy, página HTML de erro).
function mensagemAmigavel(status, mensagem) {
  const textoUtil = typeof mensagem === 'string' && mensagem.length < 300 && !/<html|Exception|SQL \[/i.test(mensagem);
  if (textoUtil) return mensagem;
  if (status === 403) return 'Você não tem permissão para fazer isso.';
  if (status === 404) return 'O registro não foi encontrado. Atualize a página e tente de novo.';
  if (status >= 500) return 'Ocorreu um erro no servidor. Tente novamente; se continuar, avise o administrador.';
  return 'Não foi possível concluir a operação. Confira os dados e tente novamente.';
}

async function requisitar(caminho, metodo = 'GET', corpo) {
  let resposta;
  try {
    resposta = await fetch(URL_API + caminho, {
      method: metodo,
      headers: { 'Content-Type': 'application/json', ...Autenticacao.cabecalhos() },
      body: corpo ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    throw new Error(`Não foi possível conectar à API (${URL_API || location.origin}). Ela está rodando?`);
  }
  const texto = await resposta.text();
  let dados = null;
  try { dados = texto ? JSON.parse(texto) : null; } catch { dados = texto; }

  if (resposta.status === 401 && !caminho.startsWith('/auth/')) Autenticacao.sair();
  if (!resposta.ok) {
    const mensagem = (dados && typeof dados === 'object' ? dados.message : dados) || `Erro ${resposta.status} em ${metodo} ${caminho}`;
    throw new Error(mensagemAmigavel(resposta.status, mensagem));
  }
  return dados;
}

// ---------- Camada de tradução tela <-> API ----------
const somenteDigitos = (valor) => (valor === undefined || valor === null ? valor : String(valor).replace(/\D/g, ''));
const ouNulo = (valor) => (valor === undefined || valor === '' ? null : valor);

function dadosDePessoa(r) {
  return {
    nome: r.nome, email: ouNulo(r.email), telefone: somenteDigitos(r.telefone), cep: somenteDigitos(r.cep),
    endereco: r.endereco, numero: r.numero, cidade: r.cidade, estado: r.estado,
  };
}

// Recurso REST comum: GET/POST na coleção, PUT/DELETE no item.
function recursoApi({ caminho, idDe, caminhoItem = (r) => `${caminho}/${r.id}`, paraTela = (j) => j, paraApi = (r) => r }) {
  return {
    async listar() {
      return (await requisitar(caminho)).map((json) => ({ ...paraTela(json), id: idDe(json) }));
    },
    async salvar(registro) {
      const corpo = await paraApi(registro);
      return registro.id ? requisitar(caminhoItem(registro), 'PUT', corpo) : requisitar(caminho, 'POST', corpo);
    },
    excluir: (registro) => requisitar(caminhoItem(registro), 'DELETE'),
  };
}

// Converte o cliente escolhido (id) em pessoaFisicaCod OU pessoaJuridicaCod.
async function donoPorCliente(clienteId) {
  const cliente = (await API.clientes.listar()).find((c) => c.id === clienteId);
  if (!cliente) throw new Error('Cliente não encontrado. Atualize a página e tente de novo.');
  return cliente.tipoCliente === 'FISICA'
    ? { pessoaFisicaCod: cliente.id, pessoaJuridicaCod: null }
    : { pessoaFisicaCod: null, pessoaJuridicaCod: cliente.id };
}

const clienteDe = (json) => json.pessoaFisicaCod?.codPessoa ?? json.pessoaJuridicaCod?.codPessoa ?? null;

const API = {
  // "Cliente" = pessoa física (que não seja prestador) ou pessoa jurídica.
  clientes: {
    async listar() {
      const [fisicas, juridicas, prestadores] = await Promise.all(
        ['/PessoasFisicas', '/PessoasJuridicas', '/PrestadoresServicos'].map((c) => requisitar(c)));
      const idsPrestadores = new Set(prestadores.map((p) => p.codPessoa));
      return [
        ...fisicas.filter((p) => !idsPrestadores.has(p.codPessoa)).map((p) => ({ ...p, id: p.codPessoa, tipoCliente: 'FISICA' })),
        ...juridicas.map((p) => ({ ...p, id: p.codPessoa, tipoCliente: 'JURIDICA' })),
      ];
    },
    salvar(r) {
      const fisica = r.tipoCliente === 'FISICA';
      const caminho = fisica ? '/PessoasFisicas' : '/PessoasJuridicas';
      const corpo = fisica
        ? { ...dadosDePessoa(r), cpf: somenteDigitos(r.cpf), rg: ouNulo(r.rg), dataNascimento: r.dataNascimento }
        : { ...dadosDePessoa(r), cnpj: somenteDigitos(r.cnpj), razaoSocial: r.razaoSocial };
      return r.id ? requisitar(`${caminho}/${r.id}`, 'PUT', corpo) : requisitar(caminho, 'POST', corpo);
    },
    excluir: (r) => requisitar(`${r.tipoCliente === 'FISICA' ? '/PessoasFisicas' : '/PessoasJuridicas'}/${r.id}`, 'DELETE'),
  },

  funcionarios: recursoApi({
    caminho: '/PrestadoresServicos',
    idDe: (j) => j.codPessoa,
    paraTela: (j) => ({ ...j, administrador: j.administrador ? 'SIM' : 'NAO' }),
    paraApi: (r) => ({
      ...dadosDePessoa(r), cpf: somenteDigitos(r.cpf), rg: ouNulo(r.rg), dataNascimento: r.dataNascimento,
      pagamento: r.pagamento, administrador: r.administrador === 'SIM',
      ...(r.senha ? { senha: r.senha } : {}),
    }),
  }),

  contratos: recursoApi({
    caminho: '/Contratos',
    idDe: (j) => j.codContrato,
    paraTela: (j) => ({ ...j, clienteId: clienteDe(j), status: STATUS_CONTRATO[(j.status ?? 1) - 1] }),
    paraApi: async (r) => ({
      descricao: r.descricao, tipoContrato: r.tipoContrato, dataInicio: r.dataInicio, dataFim: r.dataFim,
      obs: ouNulo(r.obs), status: STATUS_CONTRATO.indexOf(r.status) + 1, ...(await donoPorCliente(r.clienteId)),
    }),
  }),

  equipamentos: recursoApi({
    caminho: '/Equipamentos',
    idDe: (j) => j.codEquipamento,
    paraApi: (r) => ({ btus: r.btus, modelo: r.modelo, marca: r.marca, obs: ouNulo(r.obs) }),
  }),

  // Chave composta: o id de tela é "contrato-equipamento".
  contratosEquipamentos: recursoApi({
    caminho: '/ContratosEquipamentos',
    idDe: (j) => `${j.id.contratoCod}-${j.id.equipamentoCod}`,
    caminhoItem: (r) => `/ContratosEquipamentos/${String(r.id).replace('-', '/')}`,
    paraTela: (j) => ({ contratoId: j.id.contratoCod, equipamentoId: j.id.equipamentoCod, qtd: j.qtd }),
    paraApi: (r) => ({ contratoCod: r.contratoId, equipamentoCod: r.equipamentoId, qtd: r.qtd }),
  }),

  ordensServico: recursoApi({
    caminho: '/OrdensServico',
    idDe: (j) => j.codOs,
    paraTela: (j) => ({
      ...j, clienteId: clienteDe(j), contratoId: j.contratoCod?.codContrato ?? null,
      funcionarioId: j.prestadorServicoCod?.codPessoa ?? null, status: STATUS_OS[j.status - 1],
    }),
    paraApi: async (r) => ({
      tipoOs: r.tipoOs, valor: ouNulo(r.valor), obs: ouNulo(r.obs), dataAbertura: r.dataAbertura,
      status: STATUS_OS.indexOf(r.status) + 1, descricaoProblema: r.descricaoProblema,
      descricaoExecutado: ouNulo(r.descricaoExecutado), dataExecucao: ouNulo(r.dataExecucao),
      contratoCod: ouNulo(r.contratoId), prestadorServicoCod: ouNulo(r.funcionarioId),
      ...(await donoPorCliente(r.clienteId)),
    }),
  }),

  // Chave composta: o id de tela é "os-servico".
  servicosRealizados: recursoApi({
    caminho: '/ServicosRealizados',
    idDe: (j) => `${j.id.osCod}-${j.id.servicoCod}`,
    caminhoItem: (r) => `/ServicosRealizados/${String(r.id).replace('-', '/')}`,
    paraTela: (j) => ({
      osId: j.id.osCod, servicoId: j.id.servicoCod, qtd: j.qtd, valor: j.valor,
      desconto: j.desconto, formaPagamento: j.formaPagamento, obs: j.obs,
    }),
    paraApi: (r) => ({
      osCod: r.osId, servicoCod: r.servicoId, qtd: r.qtd, valor: r.valor,
      desconto: r.desconto ?? 0, formaPagamento: r.formaPagamento, obs: ouNulo(r.obs),
    }),
  }),

  servicos: recursoApi({
    caminho: '/Servicos',
    idDe: (j) => j.codServico,
    paraApi: (r) => ({ descricao: r.descricao, valorServico: r.valorServico, dataInicio: r.dataInicio, dataFim: r.dataFim }),
  }),
};

const Repositorio = {
  listar: (recurso) => API[recurso].listar(),
  async salvar(recurso, registro) {
    Autenticacao.exigirAdministrador();
    return API[recurso].salvar(registro);
  },
  async excluir(recurso, registro) {
    Autenticacao.exigirAdministrador();
    return API[recurso].excluir(registro);
  },

  // Cards do dashboard: VIEW VW_DASHBOARD_CARTOES + FUNCTIONS de faturamento e ticket médio (no banco).
  dashboardCartoes(inicio, fim) {
    const parametros = new URLSearchParams();
    if (inicio) parametros.set('inicio', inicio);
    if (fim) parametros.set('fim', fim);
    const consulta = parametros.toString();
    return requisitar(`/dashboard/cartoes${consulta ? `?${consulta}` : ''}`);
  },

  // Tabela "Contratos vencendo": VIEW VW_CONTRATOS_VENCENDO_30_DIAS.
  contratosVencendo: () => requisitar('/dashboard/contratos-vencendo'),

  // Tabela "OS: cliente e prestador": VIEW VW_ORDENS_SERVICO_PESSOAS (filtrada pela data de abertura).
  ordensServicoPessoas: (inicio) =>
    requisitar(`/dashboard/ordens-servico-pessoas${inicio ? `?inicio=${inicio}` : ''}`),

  // Tabela "Resumo por prestador": FUNCTION de variável tipo tabela FC_RESUMO_PRESTADORES(@inicio, @fim).
  resumoPrestadores(inicio, fim) {
    const parametros = new URLSearchParams();
    if (inicio) parametros.set('inicio', inicio);
    if (fim) parametros.set('fim', fim);
    const consulta = parametros.toString();
    return requisitar(`/dashboard/resumo-prestadores${consulta ? `?${consulta}` : ''}`);
  },

  // Busca "Histórico de serviços do cliente": FUNCTION de tabela FC_HISTORICO_SERVICOS(@NOME).
  historicoCliente: (nome) => requisitar(`/dashboard/historico-cliente?nome=${encodeURIComponent(nome)}`),
};

// ---------- Menu lateral ----------
const LINKS_MENU = [
  ['index.html', 'Início'], ['templates/Dashboard.html', 'Dashboard'], ['templates/Cliente.html', 'Clientes'],
  ['templates/Contrato.html', 'Contratos'], ['templates/Servico.html', 'Serviços / OS'], ['templates/Funcionario.html', 'Funcionários'], ['templates/Relatorio.html', 'Relatórios'],
];

function montarMenu() {
  const paginaAtual = location.pathname.split('/').pop() || 'index.html';
  const administrador = Autenticacao.ehAdministrador();
  const prefixo = location.pathname.toLowerCase().includes('/templates/') ? '../' : '';
  document.querySelectorAll('[data-admin-only]').forEach((item) => { item.hidden = !administrador; });
  const links = LINKS_MENU
    .filter(([, nome]) => administrador || !['Dashboard', 'Relatórios'].includes(nome))
    .map(([arquivo, nome]) => {
      const ativo = arquivo.split('/').pop().toLowerCase() === paginaAtual.toLowerCase();
      return `<a href="${prefixo}${arquivo}" class="${ativo ? 'ativo' : ''}">${nome}</a>`;
    }).join('');
  const usuario = Autenticacao.sessao()?.usuario ?? {};
  document.getElementById('menu').innerHTML = `<h1 class="marca-logo" role="img" aria-label="AR Ecológico"></h1><div class="links">${links}</div>
    <div class="usuario">Olá, <strong>${escapar(usuario.nome ?? 'visitante')}</strong>
      <button type="button" class="secundario" data-sair>Sair</button></div>`;
  document.querySelector('[data-sair]').addEventListener('click', () => Autenticacao.sair());
}

// ---------- Formatação ----------
const escapar = (texto) => String(texto).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const formatarMoeda = (valor) => Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const formatarData = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('pt-BR');
// Texto de tela para valores gravados sem acento no banco.
const ROTULOS_OPCOES = {
  'Manutencao Preventiva': 'Manutenção preventiva', 'Manutencao Corretiva': 'Manutenção corretiva',
  Instalacao: 'Instalação', 'Recarga de Gas': 'Recarga de gás', 'Locacao de Equipamentos': 'Locação de equipamentos',
  PMOC: 'PMOC', PIX: 'PIX', 'Cartao de Credito': 'Cartão de crédito', 'Cartao de Debito': 'Cartão de débito',
  Transferencia: 'Transferência', NAO: 'Não', SIM: 'Sim',
};
// Códigos em MAIÚSCULAS (ex.: EM_ANDAMENTO) viram "em andamento"; textos normais ficam como estão.
const formatarOpcao = (texto) => ROTULOS_OPCOES[texto]
  ?? (/^[A-Z_]+$/.test(texto) ? texto.replace(/_/g, ' ').toLowerCase() : texto);

function formatarCelula(definicao, valor, rotulos) {
  if (valor === undefined || valor === null || valor === '') return '-';
  if (definicao.origem) return rotulos[definicao.nome].get(valor) ?? `#${valor}`;
  if (MASCARAS[definicao.formato]) return MASCARAS[definicao.formato](somenteDigitos(valor));
  if (definicao.moeda) return formatarMoeda(valor);
  if (definicao.tipo === 'date') return formatarData(valor);
  if (definicao.opcoes) return formatarOpcao(String(valor));
  return valor;
}

// Para campos que apontam para outra entidade (ex.: clienteId), monta o mapa "id -> rótulo".
async function carregarRotulos(entidade) {
  const rotulos = {};
  for (const definicao of entidade.campos.filter((c) => c.origem)) {
    const origem = ENTIDADES[definicao.origem];
    const lista = await Repositorio.listar(origem.recurso);
    rotulos[definicao.nome] = new Map(lista.map((item) => [item.id, rotuloDe(origem, item)]));
  }
  return rotulos;
}

// Na tabela da tela o status vira uma etiqueta colorida (no PDF continua texto simples).
function celulaDaTabela(definicao, valor, rotulos) {
  const texto = escapar(formatarCelula(definicao, valor, rotulos));
  return definicao.nome === 'status' && valor ? `<span class="selo selo-${valor}">${texto}</span>` : texto;
}

// ---------- Máscaras (SÓ NA TELA) ----------
// O banco guarda CPF/CNPJ/CEP/telefone apenas com dígitos. A máscara só formata
// o que o usuário vê; antes de enviar, a camada API (dadosDePessoa / clientes.salvar /
// funcionarios) passa tudo por somenteDigitos(), que remove ".", "-", "/", "(" e ")".
function aplicarPadrao(digitos, padrao) {
  let saida = '';
  let i = 0;
  for (const caractere of padrao) {
    if (i >= digitos.length) break;
    saida += caractere === '0' ? digitos[i++] : caractere;
  }
  return saida;
}

const MASCARAS = {
  cpf: (d) => aplicarPadrao(d.slice(0, 11), '000.000.000-00'),
  cnpj: (d) => aplicarPadrao(d.slice(0, 14), '00.000.000/0000-00'),
  cep: (d) => aplicarPadrao(d.slice(0, 8), '00000-000'),
  telefone: (d) => aplicarPadrao(d.slice(0, 11), d.length > 10 ? '(00) 00000-0000' : '(00) 0000-0000'),
};

// ---------- Validações ----------
function cpfValido(cpf) {
  if (!/^\d{11}$/.test(cpf) || /^(\d)\1{10}$/.test(cpf)) return false;
  const digito = (n) => {
    let soma = 0;
    for (let i = 0; i < n; i++) soma += Number(cpf[i]) * (n + 1 - i);
    const resto = (soma * 10) % 11;
    return resto === 10 ? 0 : resto;
  };
  return digito(9) === Number(cpf[9]) && digito(10) === Number(cpf[10]);
}

function cnpjValido(cnpj) {
  if (!/^\d{14}$/.test(cnpj) || /^(\d)\1{13}$/.test(cnpj)) return false;
  const digito = (n) => {
    const pesos = n === 12 ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    const soma = pesos.reduce((total, peso, i) => total + Number(cnpj[i]) * peso, 0);
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  return digito(12) === Number(cnpj[12]) && digito(13) === Number(cnpj[13]);
}

const hojeIso = () => new Date().toLocaleDateString('sv-SE');

// Cada validador recebe o valor digitado e o valor que já estava gravado.
// CPF/CNPJ: os dígitos verificadores só são conferidos se o número foi ALTERADO,
// para não travar a edição de cadastros antigos/de teste (os inserts usam números fictícios).
const VALIDADORES = {
  cpf: (valor, original) => {
    const d = somenteDigitos(valor);
    if (d.length !== 11) return 'O CPF deve ter 11 dígitos.';
    if (d !== somenteDigitos(original ?? '') && !cpfValido(d)) return 'CPF inválido. Confira os números digitados.';
    return '';
  },
  cnpj: (valor, original) => {
    const d = somenteDigitos(valor);
    if (d.length !== 14) return 'O CNPJ deve ter 14 dígitos.';
    if (d !== somenteDigitos(original ?? '') && !cnpjValido(d)) return 'CNPJ inválido. Confira os números digitados.';
    return '';
  },
  cep: (valor) => (somenteDigitos(valor).length === 8 ? '' : 'O CEP deve ter 8 dígitos.'),
  telefone: (valor) => ([10, 11].includes(somenteDigitos(valor).length)
    ? '' : 'Informe o telefone com DDD (10 ou 11 dígitos).'),
  email: (valor) => (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor) ? '' : 'Digite um e-mail válido, como nome@empresa.com.'),
  nascimento: (valor) => {
    if (valor > hojeIso()) return 'A data de nascimento não pode estar no futuro.';
    if (valor < '1900-01-01') return 'Confira o ano da data de nascimento.';
    return '';
  },
  senha: (valor) => (valor.length >= 6 ? '' : 'A senha deve ter pelo menos 6 caracteres.'),
};

// "Valor (R$)" -> "Valor"; "Contrato (opcional)" -> "Contrato".
const nomeDoCampo = (definicao) => definicao.rotulo.replace(/\s*\(.*\)\s*$/, '').replace(/\?$/, '');

// Devolve { nomeDoCampo: 'mensagem' } com todos os problemas encontrados.
function validarFormulario(entidade, formulario, originais, registro) {
  const erros = {};
  for (const definicao of entidade.campos.filter((c) => c.tipo !== 'hidden')) {
    const elemento = formulario.elements[definicao.nome];
    // Campo escondido por condição (ex.: CNPJ para pessoa física) ou fixo na edição: não valida.
    if (!elemento || elemento.disabled) continue;
    const valor = elemento.value.trim();
    const nome = nomeDoCampo(definicao);

    if (definicao.tipo === 'number' && elemento.validity?.badInput) {
      erros[definicao.nome] = `Digite um número válido em "${nome}".`;
      continue;
    }
    if (!valor) {
      if (elemento.required) {
        erros[definicao.nome] = definicao.tipo === 'select' ? `Escolha uma opção em "${nome}".` : `Preencha o campo "${nome}".`;
      }
      continue;
    }
    if (definicao.tipo === 'number') {
      const numero = Number(valor);
      if (!Number.isFinite(numero)) erros[definicao.nome] = `Digite um número válido em "${nome}".`;
      else if (definicao.inteiro && !Number.isInteger(numero)) erros[definicao.nome] = `"${nome}" deve ser um número inteiro.`;
      else if (definicao.minimo !== undefined && numero < definicao.minimo) {
        erros[definicao.nome] = definicao.minimo === 0 ? `"${nome}" não pode ser negativo.` : `"${nome}" deve ser no mínimo ${definicao.minimo}.`;
      }
      continue;
    }
    const mensagem = VALIDADORES[definicao.formato]?.(valor, originais[definicao.nome]);
    if (mensagem) erros[definicao.nome] = mensagem;
  }
  // Regras entre campos só para campos que ainda não têm erro.
  for (const [nomeCampo, mensagem] of Object.entries(entidade.regras?.(registro) ?? {})) {
    erros[nomeCampo] ??= mensagem;
  }
  return erros;
}

function limparErroDoCampo(elemento) {
  elemento.removeAttribute('aria-invalid');
  const aviso = elemento.closest('label')?.querySelector('[data-erro-campo]');
  if (aviso) aviso.textContent = '';
}

// Mostra cada mensagem embaixo do campo, marca em vermelho e leva o cursor ao primeiro erro.
function mostrarErrosNoFormulario(formulario, erros) {
  formulario.querySelectorAll('input, select, textarea').forEach(limparErroDoCampo);
  let primeiro = null;
  for (const [nome, mensagem] of Object.entries(erros)) {
    const elemento = formulario.elements[nome];
    if (!elemento?.closest) continue;
    elemento.setAttribute('aria-invalid', 'true');
    const aviso = elemento.closest('label')?.querySelector('[data-erro-campo]');
    if (aviso) aviso.textContent = mensagem;
    primeiro ??= elemento;
  }
  primeiro?.focus();
  return primeiro === null;
}

// ---------- Avisos rápidos (sucesso / erro) ----------
function mostrarAviso(texto, tipo = 'sucesso') {
  let area = document.getElementById('avisos');
  if (!area) {
    area = document.createElement('div');
    area.id = 'avisos';
    area.className = 'avisos';
    area.setAttribute('role', 'status');
    area.setAttribute('aria-live', 'polite');
    document.body.append(area);
  }
  const aviso = document.createElement('div');
  aviso.className = `aviso aviso-${tipo}`;
  aviso.textContent = texto;
  area.append(aviso);
  setTimeout(() => aviso.remove(), 4000);
}

// ---------- Construção da tela de CRUD ----------
function criarHtmlDoCampo(definicao) {
  if (definicao.tipo === 'hidden') return '';
  const condicao = definicao.somenteSe ? `data-se-campo="${definicao.somenteSe[0]}" data-se-valor="${definicao.somenteSe[1]}"` : '';
  const obrigatorio = definicao.obrigatorio ? 'required' : '';
  const extras = definicao.atributos ?? '';
  const mascara = MASCARAS[definicao.formato] ? `data-mascara="${definicao.formato}"` : '';
  const classe = definicao.tipo === 'textarea' ? 'linha-inteira' : '';
  const entrada = definicao.tipo === 'select'
    ? `<select name="${definicao.nome}" ${obrigatorio}></select>`
    : definicao.tipo === 'textarea'
      ? `<textarea name="${definicao.nome}" rows="2" ${obrigatorio} ${extras}></textarea>`
      : `<input name="${definicao.nome}" type="${definicao.tipo}" ${definicao.tipo === 'number' && !extras.includes('step') ? 'step="0.01"' : ''} ${obrigatorio} ${mascara} ${extras}>`;
  const asterisco = definicao.obrigatorio ? ' <span class="obrigatorio" aria-hidden="true">*</span>' : '';
  const dica = definicao.dica ? `<small class="dica-campo">${definicao.dica}</small>` : '';
  return `<label class="${classe}" ${condicao}><span>${definicao.rotulo}${asterisco}</span>${entrada}${dica}`
    + '<small class="erro-campo" data-erro-campo></small></label>';
}

function criarEsqueleto(secao, entidade) {
  const administrador = Autenticacao.ehAdministrador();
  secao.classList.add('secao');
  secao.innerHTML = `
    <h2>${entidade.titulo}</h2>
    <div class="barra-ferramentas">
      <input type="search" placeholder="Buscar..." data-busca>
      ${administrador ? '<button type="button" data-novo>+ Novo</button>' : ''}
    </div>
    <p class="erro" data-erro-secao role="alert"></p>
    <div class="tabela-rolagem"><table><thead></thead><tbody></tbody></table></div>
    ${administrador ? `<dialog><form class="formulario" novalidate>
      <input type="hidden" name="id">
      <p class="nota-obrigatorio linha-inteira">Campos com <span class="obrigatorio">*</span> são obrigatórios.</p>
      ${entidade.campos.map(criarHtmlDoCampo).join('')}
      <p class="erro linha-inteira" data-erro-form role="alert"></p>
      <div class="rodape">
        <button type="button" class="secundario" data-cancelar>Cancelar</button>
        <button type="submit">Salvar</button>
      </div>
    </form></dialog>` : ''}`;
}

function iniciarTelaCrud(secao) {
  const entidade = ENTIDADES[secao.dataset.entidade];
  const administrador = Autenticacao.ehAdministrador();
  criarEsqueleto(secao, entidade);

  const corpoTabela = secao.querySelector('tbody');
  const caixaBusca = secao.querySelector('[data-busca]');
  const erroSecao = secao.querySelector('[data-erro-secao]');
  const janela = secao.querySelector('dialog');
  const formulario = secao.querySelector('form');
  const erroFormulario = secao.querySelector('[data-erro-form]');
  const camposRestritos = administrador ? [] : CAMPOS_RESTRITOS_PARA_FUNCIONARIOS[entidade.recurso] ?? [];
  const colunas = entidade.colunas
    .filter((nome) => !camposRestritos.includes(nome))
    .map((nome) => entidade.campos.find((c) => c.nome === nome));
  let registrosNaTela = [];
  let originais = {};      // valores gravados do registro em edição (para comparar CPF/CNPJ)
  let alterado = false;    // o usuário mexeu no formulário? (pergunta antes de descartar)

  secao.querySelector('thead').innerHTML =
    `<tr>${colunas.map((c) => `<th>${c.rotulo}</th>`).join('')}${administrador ? '<th>Ações</th>' : ''}</tr>`;

  async function atualizarTabela() {
    try {
      const [registros, rotulos] = await Promise.all([Repositorio.listar(entidade.recurso), carregarRotulos(entidade)]);
      registrosNaTela = registros;
      erroSecao.textContent = '';
      const termo = caixaBusca.value.toLowerCase();
      const filtrados = registros.filter((r) => {
        const visiveis = colunas.map((c) => formatarCelula(c, r[c.nome], rotulos));
        return visiveis.join(' ').toLowerCase().includes(termo);
      });

      corpoTabela.innerHTML = filtrados.length
        ? filtrados.map((registro) => `
          <tr>${colunas.map((c) => `<td data-rotulo="${c.rotulo}">${celulaDaTabela(c, registro[c.nome], rotulos)}</td>`).join('')}
            ${administrador ? `<td class="acoes">
              <button class="secundario" data-editar="${registro.id}">Editar</button>
              <button class="perigo" data-excluir="${registro.id}">Excluir</button>
            </td>` : ''}</tr>`).join('')
        : `<tr><td class="vazio" colspan="${colunas.length + (administrador ? 1 : 0)}">Nenhum registro encontrado.</td></tr>`;
    } catch (erro) {
      erroSecao.textContent = erro.message;
    }
  }

  async function preencherSelects() {
    for (const definicao of entidade.campos.filter((c) => c.tipo === 'select')) {
      const select = formulario.elements[definicao.nome];
      const origem = ENTIDADES[definicao.origem];
      const opcoes = origem
        ? (await Repositorio.listar(origem.recurso)).map((item) => [item.id, rotuloDe(origem, item)])
        : definicao.opcoes.map((opcao) => [opcao, formatarOpcao(opcao)]);
      select.innerHTML = '<option value="">Selecione...</option>' +
        opcoes.map(([valor, texto]) => `<option value="${escapar(valor)}">${escapar(texto)}</option>`).join('');
    }
  }

  // Mostra/esconde campos que dependem de outro (ex.: CPF só para pessoa física).
  function atualizarCamposCondicionais() {
    formulario.querySelectorAll('[data-se-campo]').forEach((rotulo) => {
      const visivel = formulario.elements[rotulo.dataset.seCampo].value === rotulo.dataset.seValor;
      rotulo.hidden = !visivel;
      rotulo.querySelector('input, select, textarea').disabled = !visivel;
    });
  }

  async function abrirFormulario(registro = {}) {
    Autenticacao.exigirAdministrador();
    erroSecao.textContent = '';
    try {
      await preencherSelects();
    } catch (erro) {
      erroSecao.textContent = erro.message;
      return;
    }
    formulario.reset();
    erroFormulario.textContent = '';
    mostrarErrosNoFormulario(formulario, {});
    const editando = registro.id !== undefined;
    originais = { ...registro };
    alterado = false;
    formulario.elements.id.value = registro.id ?? '';
    for (const c of entidade.campos.filter((def) => def.tipo !== 'hidden')) {
      const elemento = formulario.elements[c.nome];
      const valor = registro[c.nome] ?? '';
      // Valor gravado que não está na lista de opções (ex.: tipo digitado por outro sistema): mantém.
      if (c.tipo === 'select' && valor !== '' && ![...elemento.options].some((o) => o.value === String(valor))) {
        elemento.add(new Option(formatarOpcao(String(valor)), String(valor)));
      }
      // Dado do banco (só dígitos) aparece já formatado: 12345678901 -> 123.456.789-01.
      elemento.value = MASCARAS[c.formato] && valor !== '' ? MASCARAS[c.formato](somenteDigitos(valor)) : valor;
      const fixo = editando && c.fixoNaEdicao;
      elemento.disabled = fixo;
      elemento.toggleAttribute('data-fixo', fixo);
      if (c.obrigatorioAoCriar) elemento.required = !editando;
    }
    atualizarCamposCondicionais();
    janela.showModal();
  }

  // Fechar com alterações não salvas: pergunta antes (botão Cancelar ou tecla Esc).
  function podeFechar() {
    return !alterado || confirm('Você tem alterações não salvas. Deseja descartá-las?');
  }

  function lerFormulario() {
    const registro = {};
    const id = formulario.elements.id.value;
    if (id) registro.id = /^\d+$/.test(id) ? Number(id) : id;
    for (const c of entidade.campos.filter((def) => def.tipo !== 'hidden')) {
      const elemento = formulario.elements[c.nome];
      // Campos escondidos por condição ficam de fora; campos fixos na edição são lidos mesmo desabilitados.
      if (elemento.value === '' || (elemento.disabled && !elemento.hasAttribute('data-fixo'))) continue;
      registro[c.nome] = (c.tipo === 'number' || c.origem) ? Number(elemento.value) : elemento.value;
    }
    return registro;
  }

  if (administrador) {
    secao.querySelector('[data-novo]').addEventListener('click', () => abrirFormulario());
    secao.querySelector('[data-cancelar]').addEventListener('click', () => { if (podeFechar()) janela.close(); });
    janela.addEventListener('cancel', (evento) => { if (!podeFechar()) evento.preventDefault(); });
    formulario.addEventListener('change', atualizarCamposCondicionais);

    // Enquanto digita: aplica a máscara e apaga a mensagem de erro daquele campo.
    formulario.addEventListener('input', (evento) => {
      const elemento = evento.target;
      alterado = true;
      if (elemento.dataset.mascara) elemento.value = MASCARAS[elemento.dataset.mascara](somenteDigitos(elemento.value));
      limparErroDoCampo(elemento);
    });

    formulario.addEventListener('submit', async (evento) => {
      evento.preventDefault();
      erroFormulario.textContent = '';
      const registro = lerFormulario();
      const erros = validarFormulario(entidade, formulario, originais, registro);
      if (!mostrarErrosNoFormulario(formulario, erros)) {
        const quantidade = Object.keys(erros).length;
        erroFormulario.textContent = quantidade === 1
          ? 'Corrija o campo destacado em vermelho.' : `Corrija os ${quantidade} campos destacados em vermelho.`;
        return;
      }

      const botao = formulario.querySelector('button[type=submit]');
      const textoBotao = botao.textContent;
      botao.disabled = true;
      botao.textContent = 'Salvando…';
      try {
        await Repositorio.salvar(entidade.recurso, registro);
        alterado = false;
        janela.close();
        mostrarAviso(registro.id ? 'Alterações salvas com sucesso.' : 'Cadastro realizado com sucesso.');
        await atualizarTabela();
      } catch (erro) {
        erroFormulario.textContent = erro.message;   // mantém a janela aberta para corrigir
      } finally {
        botao.disabled = false;
        botao.textContent = textoBotao;
      }
    });
  }
  caixaBusca.addEventListener('input', atualizarTabela);

  corpoTabela.addEventListener('click', async (evento) => {
    if (!administrador) return;
    const { editar, excluir } = evento.target.dataset;
    const registro = registrosNaTela.find((r) => String(r.id) === (editar ?? excluir));
    if (!registro) return;
    if (editar) abrirFormulario(registro);
    if (!excluir) return;
    // Confirmação dizendo O QUE vai ser excluído.
    const nome = rotuloDe(entidade, registro);
    const pergunta = `${nome ? `Excluir "${nome}"?` : 'Excluir este item?'}\n\nEssa ação não pode ser desfeita.`;
    if (!confirm(pergunta)) return;
    erroSecao.textContent = '';
    try {
      await Repositorio.excluir(entidade.recurso, registro);
      mostrarAviso(nome ? `"${nome}" foi excluído.` : 'Item excluído.');
      await atualizarTabela();
    } catch (erro) {
      erroSecao.textContent = erro.message;
      mostrarAviso(erro.message, 'erro');
    }
  });

  atualizarTabela();
}

// ---------- Dashboard (calculado a partir dos dados reais) ----------
const PALETA = ['#1b7f5c', '#1f6fb2', '#c2620f', '#7b4bb3', '#c0392b', '#6f7f7a'];
const COR_STATUS = { ABERTA: '#1f6fb2', EM_ANDAMENTO: '#c2620f', CONCLUIDA: '#1b7f5c', CANCELADA: '#6f7f7a' };

const dataEmDias = (dias) => new Date(Date.now() + dias * 864e5).toLocaleDateString('sv-SE');
const somar = (lista, chave) => lista.reduce((total, item) => total + Number(item[chave] || 0), 0);
const contarPor = (lista, chave) =>
  lista.reduce((contagem, item) => ({ ...contagem, [item[chave] ?? 'N/D']: (contagem[item[chave] ?? 'N/D'] || 0) + 1 }), {});

function desenharBarras(idElemento, contagem, { rotular = formatarOpcao, valor = String } = {}) {
  const itens = Object.entries(contagem).sort((a, b) => b[1] - a[1]).slice(0, 6);
  const maior = Math.max(1, ...itens.map(([, total]) => total));
  document.getElementById(idElemento).innerHTML = itens.map(([nome, total]) => `
    <div class="barra"><span class="nome">${escapar(rotular(String(nome)))}</span>
      <div class="preenchimento" style="width:${(total / maior) * 60}%"></div><span>${valor(total)}</span></div>`).join('')
    || '<p class="vazio">Sem dados no período.</p>';
}

function desenharRosca(idElemento, contagem, cores = {}) {
  const itens = Object.entries(contagem);
  const total = itens.reduce((soma, [, quantidade]) => soma + quantidade, 0);
  const destino = document.getElementById(idElemento);
  if (!total) { destino.innerHTML = '<p class="vazio">Sem dados no período.</p>'; return; }

  let inicio = 0;
  const corDe = (nome, i) => cores[nome] ?? PALETA[i % PALETA.length];
  const fatias = itens.map(([nome, quantidade], i) => {
    const fim = inicio + (quantidade / total) * 100;
    const fatia = `${corDe(nome, i)} ${inicio}% ${fim}%`;
    inicio = fim;
    return fatia;
  });
  const legenda = itens.map(([nome, quantidade], i) =>
    `<li><i style="background:${corDe(nome, i)}"></i>${escapar(formatarOpcao(nome))} <b>${quantidade}</b></li>`).join('');
  destino.innerHTML = `<div class="rosca-area">
    <div class="rosca" style="background:conic-gradient(${fatias.join(',')})"><span>${total}</span></div>
    <ul class="legenda">${legenda}</ul></div>`;
}

function desenharFaturamentoMensal(idElemento, ordens) {
  const meses = Array.from({ length: 6 }, (_, i) => {
    const data = new Date();
    data.setDate(1);
    data.setMonth(data.getMonth() - (5 - i));
    return data.toLocaleDateString('sv-SE').slice(0, 7);
  });
  const totais = meses.map((mes) => somar(
    ordens.filter((o) => o.status === 'CONCLUIDA' && (o.dataExecucao || o.dataAbertura || '').startsWith(mes)), 'valor'));
  const maior = Math.max(1, ...totais);
  document.getElementById(idElemento).innerHTML = `<div class="colunas">${meses.map((mes, i) => `
    <div class="coluna" title="${formatarMoeda(totais[i])}">
      <small>${(totais[i] / 1000).toFixed(1).replace('.', ',')} mil</small>
      <div class="pilar" style="height:${(totais[i] / maior) * 100}%"></div>
      <small>${new Date(`${mes}-01T00:00:00`).toLocaleDateString('pt-BR', { month: 'short' }).replace('.', '')}</small>
    </div>`).join('')}</div>`;
}

function desenharTabelaResumo(idElemento, cabecalhos, linhas, textoVazio) {
  document.getElementById(idElemento).innerHTML = linhas.length
    ? `<div class="tabela-rolagem"><table><thead><tr>${cabecalhos.map((c) => `<th>${c}</th>`).join('')}</tr></thead>
        <tbody>${linhas.map((l) => `<tr>${l.map((celula, i) => `<td data-rotulo="${cabecalhos[i]}">${celula}</td>`).join('')}</tr>`).join('')}</tbody></table></div>`
    : `<p class="vazio">${textoVazio}</p>`;
}

async function montarDashboard() {
  const seletor = document.getElementById('periodo');
  const mensagem = document.getElementById('mensagemFaturamento');

  let dados;
  try {
    const chaves = ['clientes', 'contratos', 'ordensServico', 'funcionarios', 'servicos', 'servicosRealizados', 'contratosEquipamentos'];
    const listas = await Promise.all(chaves.map((chave) => Repositorio.listar(chave)));
    dados = Object.fromEntries(chaves.map((chave, i) => [chave, listas[i]]));
  } catch (erro) {
    mensagem.textContent = `Não foi possível carregar o dashboard: ${erro.message}`;
    return;
  }
  const { clientes, contratos, ordensServico: todasOrdens, funcionarios, servicos, servicosRealizados, contratosEquipamentos } = dados;

  const nomeCliente = new Map(clientes.map((c) => [c.id, c.nome]));
  const nomeFuncionario = new Map(funcionarios.map((f) => [f.id, f.nome]));
  const nomeServico = new Map(servicos.map((s) => [s.id, s.descricao]));
  const descricaoContrato = new Map(contratos.map((c) => [c.id, c.descricao]));

  // Mesma regra de FN_FATURAMENTO_PERIODO: OS concluídas, pela data de execução (ou de abertura).
  const concluidasNoPeriodo = (inicio) => todasOrdens.filter((o) =>
    o.status === 'CONCLUIDA' && (!inicio || (o.dataExecucao || o.dataAbertura || '') >= inicio));

  const equipPorContrato = {};
  for (const item of contratosEquipamentos) {
    const nome = descricaoContrato.get(item.contratoId) ?? `Contrato ${item.contratoId}`;
    equipPorContrato[nome] = (equipPorContrato[nome] || 0) + item.qtd;
  }

  let atualizacao = 0;

  async function atualizar() {
    const idAtualizacao = ++atualizacao;
    const dias = Number(seletor.value);
    const inicio = dias ? dataEmDias(-dias) : null;
    const fim = dias ? dataEmDias(0) : null;
    const ordens = inicio ? todasOrdens.filter((o) => (o.dataAbertura || '') >= inicio) : todasOrdens;
    const concluidas = concluidasNoPeriodo(inicio);

    // Cards e contratos vencendo: calculados NO BANCO (views + functions).
    let cartoes;
    let vencendo;
    let osPessoas;
    let resumoPrestadores;
    try {
      [cartoes, vencendo, osPessoas, resumoPrestadores] = await Promise.all([
        Repositorio.dashboardCartoes(inicio, fim), Repositorio.contratosVencendo(),
        Repositorio.ordensServicoPessoas(inicio), Repositorio.resumoPrestadores(inicio, fim)]);
      mensagem.textContent = '';
    } catch (erro) {
      mensagem.textContent = `Não foi possível carregar os indicadores: ${erro.message}`;
      return;
    }
    if (idAtualizacao !== atualizacao) return;   // o período mudou enquanto carregava

    document.getElementById('cartoes').innerHTML = [
      ['Clientes', cartoes.totalClientes, 'Cliente.html'],
      ['Contratos ativos', cartoes.contratosAtivos, 'Contrato.html'],
      ['OS em aberto', cartoes.osEmAberto, 'Servico.html'],
      ['Faturamento no período', formatarMoeda(cartoes.faturamento)],
      ['Ticket médio por OS', formatarMoeda(cartoes.ticketMedio)],
      ['Contratos vencendo em 30 dias', cartoes.contratosVencendo30Dias, 'Contrato.html'],
    ].map(([nome, valor, link]) => {
      const conteudo = `<small>${nome}</small><strong>${valor}</strong>`;
      return link ? `<a class="cartao" href="${link}">${conteudo}</a>` : `<div class="cartao">${conteudo}</div>`;
    }).join('');

    const porFuncionario = {};
    for (const ordem of concluidas) {
      const nome = nomeFuncionario.get(ordem.funcionarioId) ?? 'Sem funcionário';
      porFuncionario[nome] = (porFuncionario[nome] || 0) + Number(ordem.valor || 0);
    }

    const idsOs = new Set(ordens.map((o) => o.id));
    const itensDoPeriodo = servicosRealizados.filter((s) => idsOs.has(s.osId));
    const liquidoPorServico = {};
    for (const item of itensDoPeriodo) {
      const nome = nomeServico.get(item.servicoId) ?? 'Serviço';
      liquidoPorServico[nome] = (liquidoPorServico[nome] || 0) + item.qtd * item.valor - Number(item.desconto || 0);
    }
    const pagamentos = Object.fromEntries(funcionarios.map((f) => [f.nome, Number(f.pagamento || 0)]));

    desenharFaturamentoMensal('graficoFaturamento', todasOrdens);
    desenharRosca('graficoStatus', contarPor(ordens, 'status'), COR_STATUS);
    desenharBarras('graficoTipoOs', contarPor(ordens, 'tipoOs'));
    desenharRosca('graficoPagamento', contarPor(itensDoPeriodo, 'formaPagamento'));
    desenharBarras('graficoFuncionarios', porFuncionario, { rotular: (nome) => nome, valor: formatarMoeda });
    desenharBarras('graficoTipoContrato', contarPor(contratos, 'tipoContrato'));
    desenharBarras('graficoCidade', contarPor(clientes, 'cidade'), { rotular: (nome) => nome });
    desenharRosca('graficoTipoCliente', contarPor(clientes, 'tipoCliente'));
    desenharBarras('graficoPagamentoFuncionarios', pagamentos, { rotular: (nome) => nome, valor: formatarMoeda });
    desenharBarras('graficoServicos', liquidoPorServico, { rotular: (nome) => nome, valor: formatarMoeda });
    desenharBarras('graficoEquipamentos', equipPorContrato, { rotular: (nome) => nome });

    const recentes = [...ordens].sort((a, b) => (b.dataAbertura || '').localeCompare(a.dataAbertura || '')).slice(0, 5);
    desenharTabelaResumo('ultimasOs',
      ['Cliente', 'Problema', 'Funcionário', 'Status', 'Abertura', 'Execução', 'Valor'],
      recentes.map((o) => [
        escapar(nomeCliente.get(o.clienteId) ?? '-'), escapar(o.descricaoProblema),
        escapar(nomeFuncionario.get(o.funcionarioId) ?? '-'),
        `<span class="selo selo-${o.status}">${formatarOpcao(o.status)}</span>`,
        o.dataAbertura ? formatarData(o.dataAbertura) : '-',
        o.dataExecucao ? formatarData(o.dataExecucao) : '-',
        o.valor ? formatarMoeda(o.valor) : '-',
      ]),
      'Nenhuma OS no período.');
    desenharTabelaResumo('contratosVencendo', ['Contrato', 'Cliente', 'Termina em', 'Dias restantes'],
      vencendo.map((c) => [escapar(c.descricao), escapar(c.nomeCliente), formatarData(c.dataFim), c.diasRestantes]),
      'Nenhum contrato vence nos próximos 30 dias.');

    // A view devolve o status com espaço ("EM ANDAMENTO"); o selo usa EM_ANDAMENTO.
    desenharTabelaResumo('osPessoas',
      ['OS', 'Cliente', 'Tipo', 'Documento', 'Tipo de OS', 'Status', 'Abertura', 'Prestador', 'Valor'],
      osPessoas.slice(0, 10).map((o) => {
        const status = (o.status || '').replace(/ /g, '_');
        return [
          o.codOs, escapar(o.nomeCliente), formatarOpcao(o.tipoPessoa), escapar(o.documento ?? '-'),
          escapar(o.tipoOs), `<span class="selo selo-${status}">${formatarOpcao(status)}</span>`,
          o.dataAbertura ? formatarData(o.dataAbertura) : '-', escapar(o.nomePrestador ?? '-'),
          o.valor ? formatarMoeda(o.valor) : '-',
        ];
      }),
      'Nenhuma OS no período.');

    desenharTabelaResumo('resumoPrestadores',
      ['Prestador', 'OS concluídas', 'Faturamento'],
      resumoPrestadores.map((p) => [escapar(p.nomePrestador), p.qtdConcluidas, formatarMoeda(p.faturamento)]),
      'Nenhuma OS concluída no período.');
  }

  // Histórico do cliente: busca sob demanda (não depende do período).
  const formHistorico = document.getElementById('formHistorico');
  formHistorico?.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const nome = document.getElementById('nomeHistorico').value.trim();
    const mensagemHistorico = document.getElementById('mensagemHistorico');
    mensagemHistorico.textContent = '';
    try {
      const historico = await Repositorio.historicoCliente(nome);
      desenharTabelaResumo('historicoCliente',
        ['OS', 'Cliente', 'Cidade', 'Documento', 'Tipo de OS', 'Status', 'Abertura', 'Execução', 'Valor', 'O que foi feito'],
        historico.map((h) => {
          const status = (h.status || '').replace(/ /g, '_');
          return [
            h.codOs, escapar(h.nomeCliente), escapar(h.cidade ?? '-'), escapar(h.documento ?? '-'), escapar(h.tipoOs),
            `<span class="selo selo-${status}">${formatarOpcao(status)}</span>`,
            h.dataAbertura ? formatarData(h.dataAbertura) : '-',
            h.dataExecucao ? formatarData(h.dataExecucao) : '-',
            h.valor ? formatarMoeda(h.valor) : '-', escapar(h.oQueFoiFeito ?? '-'),
          ];
        }),
        `Nenhuma OS encontrada para "${escapar(nome)}".`);
    } catch (erro) {
      mensagemHistorico.textContent = `Não foi possível buscar o histórico: ${erro.message}`;
    }
  });

  seletor.addEventListener('change', atualizar);
  atualizar();
}

// ---------- Inicialização ----------
montarMenu();
document.querySelectorAll('[data-entidade]').forEach(iniciarTelaCrud);
if (document.getElementById('cartoes')) montarDashboard();
