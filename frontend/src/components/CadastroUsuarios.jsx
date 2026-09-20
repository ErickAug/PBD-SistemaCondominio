import { useState, useEffect } from 'react'
import { criarUsuario, listarBlocos, listarUnidades } from '../services/api.js'

const PERFIS = [
  { valor: 'SINDICO', rotulo: 'Síndico' },
  { valor: 'PORTARIA', rotulo: 'Portaria' },
  { valor: 'MORADOR', rotulo: 'Morador' },
  { valor: 'PROPRIETARIO', rotulo: 'Proprietário' },
]

const PERFIS_COM_UNIDADE = ['MORADOR', 'PROPRIETARIO']

const FORM_VAZIO = {
  nome: '',
  usuario: '',
  senha: '',
  perfil: 'SINDICO',
  blocoId: '',
  unidadeId: '',
}

function CadastroUsuarios({ administradoraId, condominios, condominioAtivoId }) {
  const [usuarios, setUsuarios] = useState([])
  const [form, setForm] = useState(FORM_VAZIO)
  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const precisaDeUnidade = PERFIS_COM_UNIDADE.includes(form.perfil)
  const condominioAtivo = condominios.find((c) => c.id === condominioAtivoId)

  useEffect(() => {
    setForm(FORM_VAZIO)
    setUnidades([])

    if (!condominioAtivoId) {
      setBlocos([])
      return
    }

    listarBlocos(condominioAtivoId)
      .then(setBlocos)
      .catch((e) => setErro(e.message))
  }, [condominioAtivoId])

  useEffect(() => {
    if (!precisaDeUnidade || !form.blocoId) {
      setUnidades([])
      return
    }

    listarUnidades(condominioAtivoId, form.blocoId)
      .then(setUnidades)
      .catch((e) => setErro(e.message))
  }, [form.blocoId, precisaDeUnidade, condominioAtivoId])

  const usuariosDoCondominioAtivo = usuarios.filter(
    (u) => u.condominioId === condominioAtivoId
  )

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => {
      if (name === 'perfil') {
        return { ...anterior, perfil: value, blocoId: '', unidadeId: '' }
      }
      if (name === 'blocoId') {
        return { ...anterior, blocoId: value, unidadeId: '' }
      }
      return { ...anterior, [name]: value }
    })
    setErro('')
  }

  async function aoEnviar(evento) {
    evento.preventDefault()
    setErro('')

    if (precisaDeUnidade && !form.unidadeId) {
      setErro('Selecione a unidade desse morador/proprietário.')
      return
    }

    setEnviando(true)
    try {
      const usuarioCriado = await criarUsuario(administradoraId, {
        nome: form.nome,
        usuario: form.usuario,
        senha: form.senha,
        perfil: form.perfil,
        condominioId: condominioAtivoId,
        unidadeId: precisaDeUnidade ? Number(form.unidadeId) : null,
      })

      setUsuarios((anterior) => [
        ...anterior,
        { ...usuarioCriado, condominioId: condominioAtivoId },
      ])
      setForm(FORM_VAZIO)
    } catch (e) {
      setErro(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section>
      <h2>Cadastro de usuários</h2>
      <p className="descricao">
        Não há auto-cadastro: só a administradora cria acessos. Cada
        usuário pertence a exatamente um condomínio e um perfil. Você
        está vendo os usuários de{' '}
        <strong>{condominioAtivo?.nome ?? '—'}</strong> — troque o
        condomínio ativo na barra lateral pra ver outro.
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
            <span>Perfil</span>
            <select name="perfil" value={form.perfil} onChange={atualizarCampo}>
              {PERFIS.map((p) => (
                <option key={p.valor} value={p.valor}>
                  {p.rotulo}
                </option>
              ))}
            </select>
          </label>

          {precisaDeUnidade && (
            <>
              <label className="campo">
                <span>Bloco</span>
                {blocos.length === 0 ? (
                  <p className="aviso-vazio">
                    Nenhum bloco cadastrado nesse condomínio ainda.
                  </p>
                ) : (
                  <select
                    name="blocoId"
                    value={form.blocoId}
                    onChange={atualizarCampo}
                    required
                  >
                    <option value="" disabled>
                      Selecione o bloco
                    </option>
                    {blocos.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.nome}
                      </option>
                    ))}
                  </select>
                )}
              </label>

              {form.blocoId && (
                <label className="campo">
                  <span>Unidade</span>
                  {unidades.length === 0 ? (
                    <p className="aviso-vazio">
                      Nenhuma unidade cadastrada nesse bloco ainda.
                    </p>
                  ) : (
                    <select
                      name="unidadeId"
                      value={form.unidadeId}
                      onChange={atualizarCampo}
                      required
                    >
                      <option value="" disabled>
                        Selecione a unidade
                      </option>
                      {unidades.map((u) => (
                        <option key={u.id} value={u.id}>
                          Unidade {u.numero}
                        </option>
                      ))}
                    </select>
                  )}
                </label>
              )}
            </>
          )}

          {erro && <p className="erro-login">{erro}</p>}

          <button type="submit" className="botao-primario" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Cadastrar usuário'}
          </button>
        </form>
      )}

      <h3 className="lista-titulo">
        {usuariosDoCondominioAtivo.length === 0
          ? `Nenhum usuário cadastrado em ${condominioAtivo?.nome ?? 'ㅤ'} ainda`
          : `${usuariosDoCondominioAtivo.length} usuário(s) cadastrado(s) em ${condominioAtivo?.nome ?? 'ㅤ'}`}
      </h3>

      <ul className="lista-condominios">
        {usuariosDoCondominioAtivo.map((usuario) => (
          <li key={usuario.id} className="item-condominio">
            <div>
              <strong>{usuario.nome}</strong>
              <p>
                @{usuario.usuario} ·{' '}
                {PERFIS.find((p) => p.valor === usuario.perfil)?.rotulo}
                {usuario.unidadeId ? ` · unidade ${usuario.unidadeId}` : ''}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default CadastroUsuarios