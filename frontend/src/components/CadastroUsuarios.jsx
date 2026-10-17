import { useState, useEffect, useCallback } from 'react'
import { criarUsuarioNoCondominio, listarUsuariosDoCondominio } from '../services/api.js'

const PERFIS = [
  { valor: 'SINDICO', rotulo: 'Síndico' },
  { valor: 'PORTARIA', rotulo: 'Portaria' },
  { valor: 'MORADOR', rotulo: 'Morador' },
  { valor: 'PROPRIETARIO', rotulo: 'Proprietário' },
]

function rotuloDoPerfil(perfil) {
  return PERFIS.find((p) => p.valor === perfil)?.rotulo ?? perfil
}

function CadastroUsuarios({ condominios, condominioAtivoId, perfisPermitidos }) {
  const perfisDisponiveis = perfisPermitidos
    ? PERFIS.filter((p) => perfisPermitidos.includes(p.valor))
    : PERFIS

  const formVazio = {
    nome: '',
    usuario: '',
    senha: '',
    perfil: perfisDisponiveis[0].valor,
    contato: '',
  }

  const [usuarios, setUsuarios] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erro, setErro] = useState('')
  const [erroLista, setErroLista] = useState('')
  const [carregandoLista, setCarregandoLista] = useState(false)
  const [enviando, setEnviando] = useState(false)

  const condominioAtivo = condominios.find((c) => c.id === condominioAtivoId)

  const carregarUsuarios = useCallback(async () => {
    if (!condominioAtivoId) {
      setUsuarios([])
      return
    }

    setCarregandoLista(true)
    setErroLista('')
    try {
      setUsuarios(await listarUsuariosDoCondominio(condominioAtivoId))
    } catch (e) {
      setErroLista(e.message)
    } finally {
      setCarregandoLista(false)
    }
  }, [condominioAtivoId])

  useEffect(() => {
    setForm(formVazio)
    setErro('')
    carregarUsuarios()
  }, [carregarUsuarios])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErro('')
  }

  async function aoEnviar(evento) {
    evento.preventDefault()
    setErro('')
    setEnviando(true)

    try {
      const usuarioCriado = await criarUsuarioNoCondominio(condominioAtivoId, {
        nome: form.nome,
        usuario: form.usuario,
        senha: form.senha,
        perfil: form.perfil,
        contato: form.contato.trim() || null,
      })

      setUsuarios((anterior) =>
        [...anterior, usuarioCriado].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      )
      setForm(formVazio)
    } catch (e) {
      setErro(e.message)
    } finally {
      setEnviando(false)
    }
  }

  let tituloLista = `${usuarios.length} usuário(s) cadastrado(s) em ${condominioAtivo?.nome ?? 'este condomínio'}`
  if (carregandoLista) tituloLista = 'Carregando usuários...'
  else if (usuarios.length === 0) tituloLista = 'Nenhum usuário cadastrado neste condomínio ainda'

  return (
    <section>
      <h2>Cadastro de usuários</h2>
      <p className="descricao">
        Não há auto-cadastro: só quem tem permissão cria acessos. Cada
        usuário pertence a exatamente um condomínio e um perfil. Você
        está vendo os usuários de{' '}
        <strong>{condominioAtivo?.nome ?? '—'}</strong>.
      </p>

      {!condominioAtivoId ? (
        <p className="aviso-vazio">
          Selecione um condomínio na barra lateral antes de cadastrar
          usuários.
        </p>
      ) : (
        <form onSubmit={aoEnviar} className="formulario">
          <label className="campo">
            <span>Nome completo</span>
            <input
              name="nome"
              value={form.nome}
              onChange={atualizarCampo}
              placeholder="Ex: Carlos Silva"
              required
            />
          </label>

          <label className="campo">
            <span>Nome de usuário (login)</span>
            <input
              name="usuario"
              value={form.usuario}
              onChange={atualizarCampo}
              placeholder="Ex: carlos.silva"
              required
            />
          </label>

          <label className="campo">
            <span>Senha provisória</span>
            <input
              type="password"
              name="senha"
              value={form.senha}
              onChange={atualizarCampo}
              required
            />
          </label>

          <label className="campo">
            <span>Contato (opcional)</span>
            <input
              name="contato"
              value={form.contato}
              onChange={atualizarCampo}
              placeholder="Telefone ou e-mail"
            />
          </label>

          <label className="campo">
            <span>Perfil</span>
            <select name="perfil" value={form.perfil} onChange={atualizarCampo}>
              {perfisDisponiveis.map((p) => (
                <option key={p.valor} value={p.valor}>
                  {p.rotulo}
                </option>
              ))}
            </select>
          </label>

          {erro && <p className="alerta-erro">{erro}</p>}

          <button type="submit" className="botao-primario" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Cadastrar usuário'}
          </button>
        </form>
      )}

      <h3 className="lista-titulo">{tituloLista}</h3>

      {erroLista && <p className="alerta-erro">{erroLista}</p>}

      <ul className="lista-condominios">
        {usuarios.map((usuario) => (
          <li key={usuario.id} className="item-condominio">
            <div>
              <strong>{usuario.nome}</strong>
              <p>
                @{usuario.usuario}
                {usuario.contato ? ` · ${usuario.contato}` : ''}
              </p>
            </div>
            <span className="selo selo-inativo">{rotuloDoPerfil(usuario.perfil)}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default CadastroUsuarios