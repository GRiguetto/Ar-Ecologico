// =====================================================================
// Autenticação (login, sessão e proteção das telas).
// Back-end:
//   POST {URL_API}/auth/login   corpo: { "email": "...", "senha": "..." }
//   200 -> { "token": "...", "usuario": { "id": 1, "nome": "...", "email": "...", "perfil": "ADMIN" | "FUNCIONARIO" } }
//   401 -> e-mail ou senha incorretos
// Quem entra são os prestadores de serviço (tela Funcionários). Perfil ADMIN = administrador.
// =====================================================================
const CHAVE_SESSAO = 'arecologico_sessao';

// Caminho até a raiz do site, para as telas dentro de /templates/.
const RAIZ_DO_SITE = location.pathname.toLowerCase().includes('/templates/') ? '../' : '';

const Autenticacao = {
  sessao() {
    try { return JSON.parse(sessionStorage.getItem(CHAVE_SESSAO)); } catch { return null; }
  },

  ehAdministrador() {
    return String(this.sessao()?.usuario?.perfil ?? '').trim().toUpperCase() === 'ADMIN';
  },

  exigirAdministrador() {
    if (!this.ehAdministrador()) throw new Error('Apenas administradores podem alterar registros.');
  },

  cabecalhos() {
    const sessao = this.sessao();
    return sessao ? { Authorization: `Bearer ${sessao.token}` } : {};
  },

  async entrar(email, senha) {
    const resposta = await fetch(`${URL_API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha }),
    }).catch(() => { throw new Error('Servidor indisponível. Verifique se a API está rodando.'); });
    if (resposta.status === 401) throw new Error('E-mail ou senha incorretos.');
    if (!resposta.ok) throw new Error('Não foi possível entrar agora. Tente novamente.');
    sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(await resposta.json()));
  },

  sair() {
    sessionStorage.removeItem(CHAVE_SESSAO);
    location.href = `${RAIZ_DO_SITE}login.html`;
  },

  exigirLogin() {
    if (!this.sessao()) {
      location.replace(`${RAIZ_DO_SITE}login.html`);
      return;
    }
    const pagina = location.pathname.split('/').pop().toLowerCase();
    if (!this.ehAdministrador() && ['dashboard.html', 'relatorio.html'].includes(pagina)) {
      location.replace(`${RAIZ_DO_SITE}index.html`);
    }
  },
};

function iniciarTelaLogin() {
  if (Autenticacao.sessao()) { location.replace('index.html'); return; }
  const formulario = document.getElementById('formularioLogin');
  const mensagem = document.getElementById('mensagemLogin');

  formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const botao = formulario.querySelector('button');
    mensagem.textContent = '';
    botao.disabled = true;
    try {
      await Autenticacao.entrar(formulario.elements.email.value.trim(), formulario.elements.senha.value);
      location.href = 'index.html';
    } catch (erro) {
      mensagem.textContent = erro.message;
      botao.disabled = false;
    }
  });
}

if (document.getElementById('formularioLogin')) iniciarTelaLogin();
else if (!document.body.hasAttribute('data-publica')) Autenticacao.exigirLogin();
