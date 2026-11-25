import { useState } from 'react'
import { login } from '../services/api.js'
import { IconePredio } from './IconesMenu.jsx'

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
      onLogin(usuarioAutenticado)
    } catch {
      setErro('Usuário ou senha inválidos.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="tela-login">
      <div className="login-fundo" role="presentation" aria-hidden="true" />
      <div className="login-scrim" role="presentation" aria-hidden="true" />

      <div className="login-marca">
        <IconePredio className="login-marca-icone" aria-hidden="true" />
        <span className="login-marca-titulo">Condomínios</span>
      </div>

      <p className="login-tagline">
        Gestão de blocos, unidades, portaria e prestação de contas em um só lugar.
      </p>

      <div className="login-cartao-area">
        <form onSubmit={aoEnviar} className="formulario cartao-login cartao-login-vidro">
          <h2 className="login-titulo">Entrar</h2>
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
    </div>
  )
}

export default Login
