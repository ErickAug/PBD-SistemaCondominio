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

async function lancarErroComMensagem(resposta, mensagemPadrao) {
  let mensagem = mensagemPadrao
  try {
    const corpo = await resposta.json()
    mensagem = corpo?.erro || corpo?.mensagem || mensagemPadrao
  } catch {
    mensagem = mensagemPadrao
  }
  throw new Error(mensagem)
}

function urlUnidade(condominioId, blocoId, unidadeId) {
  return `${API_URL}/condominios/${condominioId}/blocos/${blocoId}/unidades/${unidadeId}`
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

  const {
    token,
    usuarioId,
    nome,
    perfil,
    administradoraId,
    condominioId,
    blocoId,
    blocoNome,
    unidadeId,
    unidadeNumero,
  } = await resposta.json()
  salvarToken(token)

  return {
    token,
    usuarioId,
    nome,
    perfil: perfil.toLowerCase(),
    administradoraId,
    condominioId,
    blocoId,
    blocoNome,
    unidadeId,
    unidadeNumero,
  }
}

export function logout() {
  removerToken()
}

export async function buscarCondominios(administradoraId) {
  const resposta = await fetch(`${API_URL}/administradoras/${administradoraId}/condominios`, {
    headers: headersAutenticados(),
  })

  if (resposta.status === 401) {
    throw new Error('Sessão expirada. Faça login novamente.')
  }

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os condomínios.')
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
    await lancarErroComMensagem(resposta, 'Não foi possível salvar o condomínio.')
  }

  return resposta.json()
}

export async function criarUsuarioNoCondominio(condominioId, dadosUsuario) {
  const resposta = await fetch(`${API_URL}/condominios/${condominioId}/usuarios`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dadosUsuario),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar o usuário.')
  }

  return resposta.json()
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
  const resposta = await fetch(urlUnidade(condominioId, blocoId, unidadeId), {
    method: 'DELETE',
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(
      resposta,
      'Não foi possível excluir: a unidade provavelmente está ocupada.'
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

export async function atribuirProprietario(condominioId, blocoId, unidadeId, proprietarioId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/proprietario`, {
    method: 'PATCH',
    headers: headersAutenticados(),
    body: JSON.stringify({ proprietarioId }),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível atribuir o proprietário.')
  }

  return resposta.json()
}

export async function registrarOcupacao(condominioId, blocoId, unidadeId, dados) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/ocupacoes`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dados),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível registrar a ocupação.')
  }

  return resposta.json()
}

export async function buscarOcupacaoAtual(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/ocupacoes/atual`, {
    headers: headersAutenticados(),
  })

  if (resposta.status === 204) return null

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar a ocupação atual.')
  }

  return resposta.json()
}

export async function buscarOcupacaoEmData(condominioId, blocoId, unidadeId, data) {
  const resposta = await fetch(
    `${urlUnidade(condominioId, blocoId, unidadeId)}/ocupacoes/em-data?data=${encodeURIComponent(data)}`,
    { headers: headersAutenticados() }
  )

  if (resposta.status === 204) return null

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível consultar a ocupação nessa data.')
  }

  return resposta.json()
}

export async function listarOcupacoes(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/ocupacoes`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar o histórico de ocupações.')
  }

  return resposta.json()
}

export async function listarUsuariosDoCondominio(condominioId) {
  const resposta = await fetch(`${API_URL}/condominios/${condominioId}/usuarios`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os usuários do condomínio.')
  }

  return resposta.json()
}

export async function listarMoradores(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/moradores`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os moradores.')
  }

  return resposta.json()
}

export async function registrarMorador(condominioId, blocoId, unidadeId, dados) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/moradores`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dados),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível registrar o morador.')
  }

  return resposta.json()
}

export async function encerrarVinculoMorador(condominioId, blocoId, unidadeId, vinculoId, dataSaida) {
  const resposta = await fetch(
    `${urlUnidade(condominioId, blocoId, unidadeId)}/moradores/${vinculoId}/encerrar`,
    {
      method: 'PATCH',
      headers: headersAutenticados(),
      body: JSON.stringify({ dataSaida }),
    }
  )

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível encerrar o vínculo do morador.')
  }
}

export async function listarDependentes(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/dependentes`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os dependentes.')
  }

  return resposta.json()
}

export async function cadastrarDependente(condominioId, blocoId, unidadeId, nome) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/dependentes`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify({ nome }),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar o dependente.')
  }

  return resposta.json()
}

export async function desativarDependente(condominioId, blocoId, unidadeId, dependenteId) {
  const resposta = await fetch(
    `${urlUnidade(condominioId, blocoId, unidadeId)}/dependentes/${dependenteId}/desativar`,
    {
      method: 'PATCH',
      headers: headersAutenticados(),
    }
  )

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível desativar o dependente.')
  }
}

export async function listarAutorizados(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/autorizados`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os autorizados.')
  }

  return resposta.json()
}

export async function cadastrarAutorizado(condominioId, blocoId, unidadeId, dados) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/autorizados`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dados),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar o autorizado.')
  }

  return resposta.json()
}

export async function listarVagas(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/vagas`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar as vagas.')
  }

  return resposta.json()
}

export async function criarVaga(condominioId, blocoId, unidadeId, dados) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/vagas`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dados),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar a vaga.')
  }

  return resposta.json()
}

export async function excluirVaga(condominioId, blocoId, unidadeId, vagaId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/vagas/${vagaId}`, {
    method: 'DELETE',
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível excluir a vaga.')
  }
}

export async function listarVeiculos(condominioId, blocoId, unidadeId) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/veiculos`, {
    headers: headersAutenticados(),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível carregar os veículos.')
  }

  return resposta.json()
}

export async function cadastrarVeiculo(condominioId, blocoId, unidadeId, dados) {
  const resposta = await fetch(`${urlUnidade(condominioId, blocoId, unidadeId)}/veiculos`, {
    method: 'POST',
    headers: headersAutenticados(),
    body: JSON.stringify(dados),
  })

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível cadastrar o veículo.')
  }

  return resposta.json()
}

export async function desativarVeiculo(condominioId, blocoId, unidadeId, veiculoId) {
  const resposta = await fetch(
    `${urlUnidade(condominioId, blocoId, unidadeId)}/veiculos/${veiculoId}/desativar`,
    {
      method: 'PATCH',
      headers: headersAutenticados(),
    }
  )

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível desativar o veículo.')
  }
}

export async function buscarVeiculoPorPlaca(condominioId, placa) {
  const resposta = await fetch(
    `${API_URL}/condominios/${condominioId}/veiculos/busca?placa=${encodeURIComponent(placa)}`,
    { headers: headersAutenticados() }
  )

  if (resposta.status === 204) return null

  if (!resposta.ok) {
    await lancarErroComMensagem(resposta, 'Não foi possível buscar a placa.')
  }

  return resposta.json()
}