import { useState } from 'react'
import Login from './components/Login.jsx'
import CadastroAdministradora from './components/CadastroAdministradora.jsx'
import CadastroCondominios from './components/CadastroCondominios.jsx'
import CadastroUsuarios from './components/CadastroUsuarios.jsx'
import AreaPerfil from './components/AreaPerfil.jsx'
import SeletorCondominio from './components/SeletorCondominio.jsx'
import './App.css'
import './components/estilos.css'


function App() {

  const [usuarioLogado, setUsuarioLogado] = useState(null)

  const [telaAtual, setTelaAtual] = useState('administradora')

  const [condominioAtivo, setCondominioAtivo] = useState('')

  const [condominios, setCondominios] = useState([])

  function aoFazerLogin(usuario) {
    setUsuarioLogado(usuario)
    setCondominioAtivo(usuario.condominios[0])
  }

  function sair() {
    setUsuarioLogado(null)
    setTelaAtual('administradora')
  }

  if (!usuarioLogado) {
    return <Login onLogin={aoFazerLogin} />
  }

  const ehAdministradora = usuarioLogado.perfil === 'administradora'

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1 className="sidebar-titulo">Condomínios</h1>
        <p className="sidebar-subtitulo">{usuarioLogado.nome}</p>

        {ehAdministradora && (
          <SeletorCondominio
            condominios={usuarioLogado.condominios}
            condominioAtivo={condominioAtivo}
            aoTrocar={setCondominioAtivo}
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
                condominios={condominios}
                setCondominios={setCondominios}
              />
            )}
            {telaAtual === 'usuarios' && (
              <CadastroUsuarios
                condominios={condominios}
                condominioAtivo={condominioAtivo}
              />
            )}
          </>
        ) : (
          <AreaPerfil usuarioLogado={usuarioLogado} />
        )}
      </main>
    </div>
  )
}

export default App