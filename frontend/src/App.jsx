import { useState, useEffect } from 'react'
import PaginaCobrancas from './components/PaginaCobrancas.jsx'
import PaginaRateio from './components/PaginaRateio.jsx'
import PaginaDespesas from './components/PaginaDespesas.jsx'
import PaginaPrestacaoContas from './components/PaginaPrestacaoContas.jsx'
import PrestacaoContasUnidade from './components/PrestacaoContasUnidade.jsx'
import Login from './components/Login.jsx'
import CadastroAdministradora from './components/CadastroAdministradora.jsx'
import CadastroCondominios from './components/CadastroCondominios.jsx'
import CadastroUsuarios from './components/CadastroUsuarios.jsx'
import AreaPerfil from './components/AreaPerfil.jsx'
import BlocosUnidades from './components/BlocosUnidades.jsx'
import MinhaUnidade from './components/MinhaUnidade.jsx'
import BuscaPlaca from './components/BuscaPlaca.jsx'
import PortariaVisitas from './components/PortariaVisitas.jsx'
import PortariaRegistros from './components/PortariaRegistros.jsx'
import SeletorCondominio from './components/SeletorCondominio.jsx'
import {
  IconeEmpresa,
  IconePredio,
  IconeUsuario,
  IconeDespesa,
  IconeBusca,
  IconeRateio,
  IconeCobranca,
  IconeRelatorio,
  IconeSair,
} from './components/IconesMenu.jsx'
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
  const ehSindico = usuarioLogado?.perfil === 'sindico'
  const ehPortaria = usuarioLogado?.perfil === 'portaria'

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
    if (usuario.perfil === 'sindico') {
      setTelaAtual('unidades')
    } else if (usuario.perfil === 'portaria') {
      setTelaAtual('portariaVisitas')
    } else {
      setTelaAtual('administradora')
    }
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
  const condominiosDoSindico = [{ id: usuarioLogado.condominioId, nome: 'Meu condomínio' }]

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
              <IconeEmpresa className="nav-icone" />
              <span>Dados da administradora</span>
            </button>
            <button
              className={telaAtual === 'condominios' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('condominios')}
            >
              <IconePredio className="nav-icone" />
              <span>Condomínios</span>
            </button>
            <button
              className={telaAtual === 'usuarios' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('usuarios')}
            >
              <IconeUsuario className="nav-icone" />
              <span>Usuários</span>
            </button>
          </nav>
        )}

        {ehSindico && (
  <nav className="sidebar-nav">
    <button
      className={telaAtual === 'unidades' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('unidades')}
    >
      <IconePredio className="nav-icone" />
      <span>Blocos e unidades</span>
    </button>
    <button
      className={telaAtual === 'usuarios' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('usuarios')}
    >
      <IconeUsuario className="nav-icone" />
      <span>Usuários</span>
    </button>
    <button
      className={telaAtual === 'despesas' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('despesas')}
    >
      <IconeDespesa className="nav-icone" />
      <span>Despesas</span>
    </button>
    <button
      className={telaAtual === 'busca' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('busca')}
    >
      <IconeBusca className="nav-icone" />
      <span>Buscar por placa</span>
    </button>
    <button
      className={telaAtual === 'rateio' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('rateio')}
    >
      <IconeRateio className="nav-icone" />
      <span>Rateio do mês</span>
    </button>
    <button
      className={telaAtual === 'cobrancas' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('cobrancas')}
    >
      <IconeCobranca className="nav-icone" />
      <span>Cobranças</span>
    </button>
    <button
      className={telaAtual === 'prestacaoContas' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('prestacaoContas')}
    >
      <IconeRelatorio className="nav-icone" />
      <span>Prestação de contas</span>
    </button>
  </nav>
)}

        {ehPortaria && (
          <nav className="sidebar-nav">
            <button
              className={telaAtual === 'portariaVisitas' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('portariaVisitas')}
            >
              <IconeUsuario className="nav-icone" />
              <span>Visitas</span>
            </button>
            <button
              className={telaAtual === 'portariaRegistros' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('portariaRegistros')}
            >
              <IconeRelatorio className="nav-icone" />
              <span>Registros</span>
            </button>
            <button
              className={telaAtual === 'portariaBusca' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('portariaBusca')}
            >
              <IconeBusca className="nav-icone" />
              <span>Buscar por placa</span>
            </button>
          </nav>
        )}

        <button className="nav-item nav-sair" onClick={sair}>
          <IconeSair className="nav-icone" />
          <span>Sair</span>
        </button>
      </aside>

      <main className="conteudo">
        <div className="aba-conteudo" key={telaAtual}>
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
                condominios={condominios}
                condominioAtivoId={condominioAtivoId}
              />
            )}
          </>
        ) : ehSindico ? (
          <>
            {telaAtual === 'unidades' && (
              <BlocosUnidades
                nomeCondominio={condominioAtivo?.nome ?? 'Meu condomínio'}
                condominioId={usuarioLogado.condominioId}
              />
            )}
            {telaAtual === 'usuarios' && (
              <CadastroUsuarios
                condominios={condominiosDoSindico}
                condominioAtivoId={usuarioLogado.condominioId}
                perfisPermitidos={['MORADOR', 'PROPRIETARIO']}
              />
            )}
            {telaAtual === 'despesas' && (
              <PaginaDespesas condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'rateio' && (
              <PaginaRateio condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'cobrancas' && (
              <PaginaCobrancas condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'busca' && (
              <BuscaPlaca condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'prestacaoContas' && (
              <PaginaPrestacaoContas condominioId={usuarioLogado.condominioId} />
            )}
          </>
        ) : ehPortaria ? (
          <>
            {telaAtual === 'portariaVisitas' && (
              <PortariaVisitas condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'portariaRegistros' && (
              <PortariaRegistros condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'portariaBusca' && (
              <BuscaPlaca condominioId={usuarioLogado.condominioId} />
            )}
          </>
        ) : usuarioLogado.perfil === 'morador' ||
          (usuarioLogado.perfil === 'proprietario' && usuarioLogado.unidadeId) ? (
          <>
            <MinhaUnidade usuarioLogado={usuarioLogado} />
            <PrestacaoContasUnidade condominioId={usuarioLogado.condominioId} />
          </>
        ) : (
          <AreaPerfil usuarioLogado={usuarioLogado} />
        )}
        </div>
      </main>
    </div>
  )
}

export default App