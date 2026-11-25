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
import LivroOcorrencias from './components/LivroOcorrencias.jsx'
import Avisos from './components/Avisos.jsx'
import MeusAvisos from './components/MeusAvisos.jsx'
import FilaOcorrenciasMorador from './components/FilaOcorrenciasMorador.jsx'
import MinhasOcorrencias from './components/MinhasOcorrencias.jsx'
import AreasComuns from './components/AreasComuns.jsx'
import MinhasReservas from './components/MinhasReservas.jsx'
import Reunioes from './components/Reunioes.jsx'
import MinhasReunioes from './components/MinhasReunioes.jsx'
import MinhaArea from './components/MinhaArea.jsx'
import SeletorUnidade from './components/SeletorUnidade.jsx'
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
  IconeLivro,
  IconeAviso,
  IconeChamado,
  IconeReserva,
  IconeReuniao,
  IconePainel,
  IconeUnidade,
  IconeSol,
  IconeLua,
  IconeChevronLateral,
  IconeSair,
} from './components/IconesMenu.jsx'
import { buscarCondominios, listarMinhasUnidades, logout as apiLogout } from './services/api.js'
import './App.css'
import './components/estilos.css'

function App() {
  const [usuarioLogado, setUsuarioLogado] = useState(null)
  const [telaAtual, setTelaAtual] = useState('administradora')
  const [condominios, setCondominios] = useState([])
  const [condominioAtivoId, setCondominioAtivoId] = useState('')
  const [erroCondominios, setErroCondominios] = useState('')
  const [minhasUnidades, setMinhasUnidades] = useState([])
  const [unidadeAtivaId, setUnidadeAtivaId] = useState('')
  const [carregandoUnidades, setCarregandoUnidades] = useState(true)
  const [tema, setTema] = useState(() => {
    try {
      return localStorage.getItem('tema') || 'claro'
    } catch {
      return 'claro'
    }
  })
  const [sidebarColapsada, setSidebarColapsada] = useState(() => {
    try {
      return localStorage.getItem('sidebarColapsada') === 'sim'
    } catch {
      return false
    }
  })

  const ehAdministradora = usuarioLogado?.perfil === 'administradora'
  const ehSindico = usuarioLogado?.perfil === 'sindico'
  const ehPortaria = usuarioLogado?.perfil === 'portaria'
  const ehMoradorOuProprietario =
    usuarioLogado?.perfil === 'morador' || usuarioLogado?.perfil === 'proprietario'

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', tema)
    try {
      localStorage.setItem('tema', tema)
    } catch {
    }
  }, [tema])

  function alternarTema() {
    setTema((temaAtual) => (temaAtual === 'escuro' ? 'claro' : 'escuro'))
  }

  useEffect(() => {
    try {
      localStorage.setItem('sidebarColapsada', sidebarColapsada ? 'sim' : 'nao')
    } catch {
    }
  }, [sidebarColapsada])

  function alternarSidebar() {
    setSidebarColapsada((colapsada) => !colapsada)
  }

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

  useEffect(() => {
    if (!usuarioLogado || !ehMoradorOuProprietario) return

    setCarregandoUnidades(true)
    listarMinhasUnidades(usuarioLogado.condominioId)
      .then((lista) => {
        setMinhasUnidades(lista)
        if (lista.length > 0) {
          setUnidadeAtivaId(lista[0].unidadeId)
        }
      })
      .catch(() => setMinhasUnidades([]))
      .finally(() => setCarregandoUnidades(false))
  }, [usuarioLogado, ehMoradorOuProprietario])

  function aoFazerLogin(usuario) {
    setUsuarioLogado(usuario)
    if (usuario.perfil === 'sindico') {
      setTelaAtual('unidades')
    } else if (usuario.perfil === 'portaria') {
      setTelaAtual('portariaOcorrencias')
    } else if (usuario.perfil === 'morador' || usuario.perfil === 'proprietario') {
      setTelaAtual('minhaArea')
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
    setMinhasUnidades([])
    setUnidadeAtivaId('')
  }

  if (!usuarioLogado) {
    return <Login onLogin={aoFazerLogin} />
  }

  const condominioAtivo = condominios.find((c) => c.id === condominioAtivoId)
  const condominiosDoSindico = [{ id: usuarioLogado.condominioId, nome: 'Meu condomínio' }]
  const unidadeAtiva = minhasUnidades.find((u) => u.unidadeId === unidadeAtivaId) || null

  return (
    <div className="layout">
      <aside className={sidebarColapsada ? 'sidebar colapsada' : 'sidebar'}>
        <div className="sidebar-cabecalho">
          <h1 className="sidebar-titulo">Condomínios</h1>
          <button
            className="botao-tema"
            onClick={alternarTema}
            aria-label={tema === 'escuro' ? 'Ativar modo claro' : 'Ativar modo escuro'}
            title={tema === 'escuro' ? 'Modo claro' : 'Modo escuro'}
          >
            {tema === 'escuro' ? <IconeSol /> : <IconeLua />}
          </button>
          <button
            className="botao-tema botao-colapsar"
            onClick={alternarSidebar}
            aria-label={sidebarColapsada ? 'Expandir menu' : 'Ocultar menu'}
            title={sidebarColapsada ? 'Expandir menu' : 'Ocultar menu'}
          >
            <IconeChevronLateral />
          </button>
        </div>
        <p className="sidebar-subtitulo">{usuarioLogado.nome}</p>

        {ehAdministradora && condominios.length > 0 && (
          <SeletorCondominio
            condominios={condominios}
            condominioAtivoId={condominioAtivoId}
            aoTrocar={setCondominioAtivoId}
          />
        )}

        {ehMoradorOuProprietario && minhasUnidades.length > 0 && (
          <SeletorUnidade
            unidades={minhasUnidades}
            unidadeAtivaId={unidadeAtivaId}
            aoTrocar={setUnidadeAtivaId}
          />
        )}

        {ehAdministradora && (
          <nav className="sidebar-nav" key={sidebarColapsada ? 'colapsada' : 'expandida'}>
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
  <nav className="sidebar-nav" key={sidebarColapsada ? 'colapsada' : 'expandida'}>
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
    <button
      className={telaAtual === 'ocorrencias' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('ocorrencias')}
    >
      <IconeLivro className="nav-icone" />
      <span>Livro de ocorrências</span>
    </button>
    <button
      className={telaAtual === 'avisos' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('avisos')}
    >
      <IconeAviso className="nav-icone" />
      <span>Avisos</span>
    </button>
    <button
      className={telaAtual === 'chamados' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('chamados')}
    >
      <IconeChamado className="nav-icone" />
      <span>Ocorrências dos moradores</span>
    </button>
    <button
      className={telaAtual === 'areasComuns' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('areasComuns')}
    >
      <IconeReserva className="nav-icone" />
      <span>Áreas comuns e reservas</span>
    </button>
    <button
      className={telaAtual === 'reunioes' ? 'nav-item ativo' : 'nav-item'}
      onClick={() => setTelaAtual('reunioes')}
    >
      <IconeReuniao className="nav-icone" />
      <span>Reuniões</span>
    </button>
  </nav>
)}

        {ehPortaria && (
          <nav className="sidebar-nav" key={sidebarColapsada ? 'colapsada' : 'expandida'}>
            <button
              className={telaAtual === 'portariaOcorrencias' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('portariaOcorrencias')}
            >
              <IconeLivro className="nav-icone" />
              <span>Livro de ocorrências</span>
            </button>
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

        {ehMoradorOuProprietario && (
          <nav className="sidebar-nav" key={sidebarColapsada ? 'colapsada' : 'expandida'}>
            <button
              className={telaAtual === 'minhaArea' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('minhaArea')}
            >
              <IconePainel className="nav-icone" />
              <span>Minha área</span>
            </button>
            <button
              className={telaAtual === 'meusAvisos' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('meusAvisos')}
            >
              <IconeAviso className="nav-icone" />
              <span>Avisos</span>
            </button>
            <button
              className={telaAtual === 'minhasOcorrencias' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('minhasOcorrencias')}
            >
              <IconeChamado className="nav-icone" />
              <span>Minhas ocorrências</span>
            </button>
            <button
              className={telaAtual === 'minhasReservas' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('minhasReservas')}
            >
              <IconeReserva className="nav-icone" />
              <span>Reservar área comum</span>
            </button>
            <button
              className={telaAtual === 'minhasReunioes' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('minhasReunioes')}
            >
              <IconeReuniao className="nav-icone" />
              <span>Reuniões</span>
            </button>
            <button
              className={telaAtual === 'minhaUnidade' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('minhaUnidade')}
            >
              <IconeUnidade className="nav-icone" />
              <span>Minha unidade</span>
            </button>
            <button
              className={telaAtual === 'prestacaoContasUnidade' ? 'nav-item ativo' : 'nav-item'}
              onClick={() => setTelaAtual('prestacaoContasUnidade')}
            >
              <IconeRelatorio className="nav-icone" />
              <span>Prestação de contas</span>
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
                perfisPermitidos={['MORADOR', 'PROPRIETARIO', 'PORTARIA']}
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
            {telaAtual === 'ocorrencias' && (
              <LivroOcorrencias condominioId={usuarioLogado.condominioId} somenteLeitura />
            )}
            {telaAtual === 'avisos' && (
              <Avisos condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'chamados' && (
              <FilaOcorrenciasMorador condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'areasComuns' && (
              <AreasComuns condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'reunioes' && (
              <Reunioes condominioId={usuarioLogado.condominioId} />
            )}
          </>
        ) : ehPortaria ? (
          <>
            {telaAtual === 'portariaOcorrencias' && (
              <LivroOcorrencias condominioId={usuarioLogado.condominioId} />
            )}
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
        ) : ehMoradorOuProprietario ? (
          <>
            {telaAtual === 'minhaArea' && (
              <MinhaArea
                condominioId={usuarioLogado.condominioId}
                unidadeAtiva={unidadeAtiva}
                carregandoUnidades={carregandoUnidades}
              />
            )}
            {telaAtual === 'meusAvisos' && (
              <MeusAvisos condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'minhasOcorrencias' && (
              <MinhasOcorrencias
                condominioId={usuarioLogado.condominioId}
                blocoId={unidadeAtiva?.blocoId}
                unidadeId={unidadeAtiva?.unidadeId}
              />
            )}
            {telaAtual === 'minhasReservas' && (
              <MinhasReservas
                condominioId={usuarioLogado.condominioId}
                blocoId={unidadeAtiva?.blocoId}
                unidadeId={unidadeAtiva?.unidadeId}
                podeUsar={unidadeAtiva?.papeis?.includes('MORADOR') ?? false}
              />
            )}
            {telaAtual === 'minhasReunioes' && (
              <MinhasReunioes condominioId={usuarioLogado.condominioId} />
            )}
            {telaAtual === 'minhaUnidade' && (
              <MinhaUnidade
                condominioId={usuarioLogado.condominioId}
                blocoId={unidadeAtiva?.blocoId}
                unidadeId={unidadeAtiva?.unidadeId}
                blocoNome={unidadeAtiva?.blocoNome}
                unidadeNumero={unidadeAtiva?.unidadeNumero}
                usuarioId={usuarioLogado.usuarioId}
                podeUsar={unidadeAtiva?.papeis?.includes('MORADOR') ?? false}
              />
            )}
            {telaAtual === 'prestacaoContasUnidade' && (
              <PrestacaoContasUnidade condominioId={usuarioLogado.condominioId} />
            )}
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