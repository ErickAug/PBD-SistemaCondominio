import { useState, useEffect } from 'react'

const PERFIS = [
  { valor: 'sindico', rotulo: 'Síndico' },
  { valor: 'portaria', rotulo: 'Portaria' },
  { valor: 'morador', rotulo: 'Morador' },
  { valor: 'proprietario', rotulo: 'Proprietário' },
]

const FORM_VAZIO = {
  nome: '',
  usuario: '',
  senha: '',
  perfil: 'sindico',
  condominioNome: '',
}

function CadastroUsuarios({ condominios, condominioAtivo }) {
  const [usuarios, setUsuarios] = useState([])
  const [form, setForm] = useState(FORM_VAZIO)
  const [erro, setErro] = useState('')

  useEffect(() => {
    setForm((anterior) => ({ ...anterior, condominioNome: condominioAtivo }))
  }, [condominioAtivo])

  const usuariosDoCondominioAtivo = usuarios.filter(
    (u) => u.condominioNome === condominioAtivo
  )

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErro('')
  }

  function cadastrarUsuario(evento) {
    evento.preventDefault()

    const jaExiste = usuarios.some((u) => u.usuario === form.usuario)
    if (jaExiste) {
      setErro('Esse nome de usuário já está cadastrado. Escolha outro.')
      return
    }

    const novoUsuario = {
      id: crypto.randomUUID(),
      ...form,
    }

    setUsuarios((anterior) => [...anterior, novoUsuario])
    setForm({ ...FORM_VAZIO, condominioNome: condominioAtivo })
  }

  return (
    <section>
      <h2>Cadastro de usuários</h2>
      <p className="descricao">
        Não há auto-cadastro: só a administradora cria acessos. Cada
        usuário pertence a exatamente um condomínio e um perfil. Você
        está vendo os usuários de <strong>{condominioAtivo}</strong> —
        troque o condomínio ativo na barra lateral pra ver outro.
      </p>

      <form onSubmit={cadastrarUsuario} className="formulario">
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

        <label className="campo">
          <span>Condomínio</span>
          {condominios.length === 0 ? (
            <p className="aviso-vazio">
              Nenhum condomínio cadastrado ainda — cadastre um na aba
              "Condomínios" antes de criar usuários.
            </p>
          ) : (
            <select
              name="condominioNome"
              value={form.condominioNome}
              onChange={atualizarCampo}
              required
            >
              <option value="" disabled>
                Selecione um condomínio
              </option>
              {condominios.map((c) => (
                <option key={c.id} value={c.nome}>
                  {c.nome}
                </option>
              ))}
            </select>
          )}
        </label>

        {erro && <p className="erro-login">{erro}</p>}

        <button
          type="submit"
          className="botao-primario"
          disabled={condominios.length === 0}
        >
          Cadastrar usuário
        </button>
      </form>

      <h3 className="lista-titulo">
        {usuariosDoCondominioAtivo.length === 0
          ? `Nenhum usuário cadastrado em ${condominioAtivo} ainda`
          : `${usuariosDoCondominioAtivo.length} usuário(s) cadastrado(s) em ${condominioAtivo}`}
      </h3>

      <ul className="lista-condominios">
        {usuariosDoCondominioAtivo.map((usuario) => (
          <li key={usuario.id} className="item-condominio">
            <div>
              <strong>{usuario.nome}</strong>
              <p>
                @{usuario.usuario} · {PERFIS.find((p) => p.valor === usuario.perfil)?.rotulo} ·{' '}
                {usuario.condominioNome}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default CadastroUsuarios