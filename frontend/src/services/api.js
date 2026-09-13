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

  const { token } = await resposta.json()
  salvarToken(token)

  return { token }
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
      'Não foi possível carregar os dados do usuário logado.'
    )
  }

  return resposta.json()
}


export async function buscarCondominios() {
  const resposta = await fetch(`${API_URL}/api/condominios`, {
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

export async function criarCondominio(dadosCondominio) {
  const resposta = await fetch(`${API_URL}/api/condominios`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dadosCondominio),
  })

  if (!resposta.ok) {
    throw new Error('Não foi possível salvar o condomínio.')
  }

  return resposta.json()
}

export async function criarUsuario(dadosUsuario) {
  const resposta = await fetch(`${API_URL}/api/usuarios`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dadosUsuario),
  })

  if (resposta.status === 409) {
    throw new Error('Esse nome de usuário já está cadastrado. Escolha outro.')
  }

  if (!resposta.ok) {
    throw new Error('Não foi possível cadastrar o usuário.')
  }

  return resposta.json()
}