import { useState } from 'react'
import { login } from '../services/api.js'

function Login({ onLogin }) {
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  async function aoEnviar(evento) {
    evento.preventDefault()
    setErro('')
    setCarregando(true)

    try {
      const usuarioAutenticado = await login(usuario, senha)

      const usuarioComDadosTemporarios = {
        ...usuarioAutenticado,
        nome: 'Admin Master (dados temporários)',
        perfil: 'administradora',
        condominios: ['Residencial Teste'],
      }

      onLogin(usuarioComDadosTemporarios)
    } catch {
      setErro('Usuário ou senha inválidos.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="tela-login">
      <form onSubmit={aoEnviar} className="formulario cartao-login">
        <h1 className="login-titulo">Entrar</h1>
        <p className="descricao">Acesso ao sistema de gestão de condomínios.</p>

        <label className="campo">
          <span>Usuário</span>
          <input
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            autoFocus
            required
          />
        </label>

        <label className="campo">
          <span>Senha</span>
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            required
          />
        </label>

        {erro && <p className="erro-login">{erro}</p>}

        <button type="submit" className="botao-primario" disabled={carregando}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}

export default Login