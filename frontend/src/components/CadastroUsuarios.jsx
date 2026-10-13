import { useState, useEffect } from 'react'
import { criarUsuarioNoCondominio } from '../services/api.js'

const PERFIS = [
  { valor: 'SINDICO', rotulo: 'Síndico' },
  { valor: 'PORTARIA', rotulo: 'Portaria' },
  { valor: 'MORADOR', rotulo: 'Morador' },
  { valor: 'PROPRIETARIO', rotulo: 'Proprietário' },
]

function CadastroUsuarios({ condominios, condominioAtivoId, perfisPermitidos }) {
  const perfisDisponiveis = perfisPermitidos
    ? PERFIS.filter((p) => perfisPermitidos.includes(p.valor))
    : PERFIS

  const formVazio = {
    nome: '',
    usuario: '',
    senha: '',
    perfil: perfisDisponiveis[0].valor,
  }

  const [usuarios, setUsuarios] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const condominioAtivo = condominios.find((c) => c.id === condominioAtivoId)

  useEffect(() => {
    setForm(formVazio)
    setErro('')
  }, [condominioAtivoId])

  const usuariosDoCondominioAtivo = usuarios.filter(
    (u) => u.condominioId === condominioAtivoId
  )

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
      })

      setUsuarios((anterior) => [
        ...anterior,
        { ...usuarioCriado, condominioId: condominioAtivoId },
      ])
      setForm(formVazio)
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
            <span>Perfil</span>
            <select name="perfil" value={form.perfil} onChange={atualizarCampo}>
              {perfisDisponiveis.map((p) => (
                <option key={p.valor} value={p.valor}>
                  {p.rotulo}
                </option>
              ))}
            </select>
          </label>

          {erro && <p className="erro-login">{erro}</p>}

          <button type="submit" className="botao-primario" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Cadastrar usuário'}
          </button>
        </form>
      )}

      <h3 className="lista-titulo">
        {usuariosDoCondominioAtivo.length === 0
          ? 'Nenhum usuário cadastrado nesta sessão'
          : `${usuariosDoCondominioAtivo.length} usuário(s) cadastrado(s) nesta sessão`}
      </h3>

      <ul className="lista-condominios">
        {usuariosDoCondominioAtivo.map((usuario) => (
          <li key={usuario.id} className="item-condominio">
            <div>
              <strong>{usuario.nome}</strong>
              <p>
                @{usuario.usuario} ·{' '}
                {PERFIS.find((p) => p.valor === usuario.perfil)?.rotulo}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default CadastroUsuarios