const API_URL = import.meta.env.VITE_API_URL

const CHAVE_TOKEN = 'token'


export function salvarToken(token) {
  localStorage.setItem(CHAVE_TOKEN, token)
}

export function obterToken() {
  return localStorage.getItem(CHAVE_TOKEN)
}

export function removerToken() {
  localStorage.removeItem(CHAVE_TOKEN)
}

function headersAutenticados() {
  const token = obterToken()
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

export async function login(usuario, senha) {
  const resposta = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ usuario, senha }),
  })

  if (!resposta.ok) {
    throw new Error('Usuário ou senha inválidos.')
  }

  const { token, usuarioId, nome, perfil, administradoraId, condominioId } = await resposta.json()
  salvarToken(token)

  return {
    token,
    usuarioId,
    nome,
    perfil: perfil.toLowerCase(),
    administradoraId,
    condominioId,
  }
}

export function logout() {
  removerToken()
}

export async function buscarUsuarioLogado() {
  const resposta = await fetch(`${API_URL}/auth/me`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    throw new Error(
      'Não foi possível carregar os dados do usuário logado (endpoint ainda não existe no backend?).'
    )
  }

  return resposta.json()
}

export async function buscarCondominios(administradoraId) {
  const resposta = await fetch(`${API_URL}/administradoras/${administradoraId}/condominios`, {
    headers: headersAutenticados(),
  })

  if (resposta.status === 401) {
    throw new Error('Sessão expirada. Faça login novamente.')
  }

  if (!resposta.ok) {
    throw new Error('Não foi possível carregar os condomínios.')
  }

  return resposta.json()
}

export async function criarCondominio(administradoraId, dadosCondominio) {
  const resposta = await fetch(`${API_URL}/administradoras/${administradoraId}/condominios`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dadosCondominio),
  })

  if (!resposta.ok) {
    throw new Error('Não foi possível salvar o condomínio.')
  }

  return resposta.json()
}

export async function criarUsuario(administradoraId, dadosUsuario) {
  const resposta = await fetch(`${API_URL}/administradoras/${administradoraId}/usuarios`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dadosUsuario),
  })

  if (resposta.status === 409) {
    throw new Error('Esse nome de usuário já está cadastrado. Escolha outro.')
  }

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar o usuário.')
  }

  return resposta.json()
}


async function lancarErroComMensagem(resposta, mensagemPadrao) {
  let mensagem = mensagemPadrao
  try {
    const corpo = await resposta.json()
    if (corpo?.mensagem) mensagem = corpo.mensagem
  } catch {
  }
  throw new Error(mensagem)
}

export async function cadastrarBloco(condominioId, nome) {
  const resposta = await fetch(`${API_URL}/condominios/${condominioId}/blocos`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify({ nome }),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar o bloco.')
  }

  return resposta.json()
}

export async function listarBlocos(condominioId) {
  const resposta = await fetch(`${API_URL}/condominios/${condominioId}/blocos`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os blocos.')
  }

  return resposta.json()
}

export async function cadastrarUnidade(condominioId, blocoId, dados) {
  const resposta = await fetch(
    `${API_URL}/condominios/${condominioId}/blocos/${blocoId}/unidades`,
    {
      method: 'POST',
      headers: headersAutenticados(),
      body: JSON.stringify(dados), 
    }
  )

  if (!resposta.ok) {
    await lancarErroComMensagem(
      resposta,
      'Não foi possível cadastrar a unidade. Confira a fração ideal e o número.'
    )
  }

  return resposta.json()
}

export async function listarUnidades(condominioId, blocoId) {
  const resposta = await fetch(
    `${API_URL}/condominios/${condominioId}/blocos/${blocoId}/unidades`,
    { headers: headersAutenticados() }
  )

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar as unidades.')
  }

  return resposta.json()
}

export async function excluirUnidade(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(
    `${API_URL}/condominios/${condominioId}/blocos/${blocoId}/unidades/${unidadeId}`,
    {
      method: 'DELETE',
      headers: headersAutenticados(),
    }
  )

  if (!resposta.ok) {
    await lancarErroComMensagem(
      resposta,
      'Não foi possível excluir: a unidade provavelmente está ocupada (tem morador vinculado).'
    )
  }
}

export async function buscarStatusFracaoIdeal(condominioId) {
  const resposta = await fetch(`${API_URL}/condominios/${condominioId}/fracao-ideal`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar o status da fração ideal.')
  }

  return resposta.json()
}