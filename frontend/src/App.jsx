import { useState, useEffect } from 'react'
import Login from './components/Login.jsx'
import CadastroAdministradora from './components/CadastroAdministradora.jsx'
import CadastroCondominios from './components/CadastroCondominios.jsx'
import CadastroUsuarios from './components/CadastroUsuarios.jsx'
import AreaPerfil from './components/AreaPerfil.jsx'
import BlocosUnidades from './components/BlocosUnidades.jsx'
import SeletorCondominio from './components/SeletorCondominio.jsx'
import { buscarCondominios, logout as apiLogout } from './services/api.js'
import './App.css'
import './components/estilos.css'

function App() {
  const [usuarioLogado, setUsuarioLogado] = useState(null)
  const [telaAtual, setTelaAtual] = useState('administradora')
  const [condominios, setCondominios] = useState([])
  const [condominioAtivoId, setCondominioAtivoId] = useState('')
  const [erroCondominios, setErroCondominios] = useState('')

  const ehAdministradora = usuarioLogado?.perfil === 'administradora'

  useEffect(() => {
    if (!usuarioLogado || !ehAdministradora) return

    buscarCondominios(usuarioLogado.administradoraId)
      .then((lista) => {
        setCondominios(lista)
        if (lista.length > 0) {
          setCondominioAtivoId(lista[0].id)
        }
      })
      .catch((erro) => setErroCondominios(erro.message))
  }, [usuarioLogado, ehAdministradora])

  function aoFazerLogin(usuario) {
    setUsuarioLogado(usuario)
  }

  function sair() {
    apiLogout()
    setUsuarioLogado(null)
    setTelaAtual('administradora')
    setCondominios([])
    setCondominioAtivoId('')
  }

  if (!usuarioLogado) {
    return <Login onLogin={aoFazerLogin} />
  }

  const condominioAtivo = condominios.find((c) => c.id === condominioAtivoId)

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1 className="sidebar-titulo">Condomínios</h1>
        <p className="sidebar-subtitulo">{usuarioLogado.nome}</p>

        {ehAdministradora && condominios.length > 0 && (
          <SeletorCondominio
            condominios={condominios}
            condominioAtivoId={condominioAtivoId}
            aoTrocar={setCondominioAtivoId}
          />
        )}

        {ehAdministradora && (
          <nav className="sidebar-nav">
            <button
              className={telaAtual === 'administradora' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('administradora')}
            >
              Dados da administradora
            </button>
            <button
              className={telaAtual === 'condominios' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('condominios')}
            >
              Condomínios
            </button>
            <button
              className={telaAtual === 'usuarios' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('usuarios')}
            >
              Usuários
            </button>
          </nav>
        )}

        <button className="nav-item nav-sair" onClick={sair}>
          Sair
        </button>
      </aside>

      <main className="conteudo">
        {ehAdministradora ? (
          <>
            {telaAtual === 'administradora' && <CadastroAdministradora />}
            {telaAtual === 'condominios' && (
              <CadastroCondominios
                administradoraId={usuarioLogado.administradoraId}
                condominios={condominios}
                setCondominios={setCondominios}
                erroCarregamento={erroCondominios}
              />
            )}
            {telaAtual === 'usuarios' && (
              <CadastroUsuarios
                administradoraId={usuarioLogado.administradoraId}
                condominios={condominios}
                condominioAtivoId={condominioAtivoId}
              />
            )}
          </>
        ) : usuarioLogado.perfil === 'sindico' ? (
          <BlocosUnidades
            nomeCondominio={condominioAtivo?.nome ?? 'Meu condomínio'}
            condominioId={usuarioLogado.condominioId}
          />
        ) : (
          <AreaPerfil usuarioLogado={usuarioLogado} />
        )}
      </main>
    </div>
  )
}

export default App