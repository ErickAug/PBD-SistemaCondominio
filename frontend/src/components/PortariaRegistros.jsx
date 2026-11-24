import { useEffect, useState, useCallback } from 'react'
import {
  listarBlocos,
  listarUnidades,
  registrarEntradaManual,
  registrarSaidaAcesso,
  listarQuemEstaDentro,
  listarQuemEstaDentroNaUnidade,
  filtrarHistoricoAcessos,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

const TIPOS_REGISTRO = ['Visita', 'Prestador de serviço', 'Entrega', 'Morador', 'Outro']

function formatarHora(dataHora) {
  if (!dataHora) return ''
  return new Date(dataHora).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function horasDesde(dataHora) {
  return (Date.now() - new Date(dataHora).getTime()) / 3600000
}

function ListaDeQuemEstaDentro({ registros, onRegistrarSaida, registrandoSaidaId, erroItem }) {
  if (registros.length === 0) {
    return <p className="aviso-vazio">Ninguém registrado como dentro no momento.</p>
  }

  return (
    <ul className="lista-condominios">
      {registros.map((r) => (
        <li key={r.id} className="item-unidade">
          <div className="item-condominio">
            <div>
              <strong>{r.nome}</strong>
              <p>
                {r.tipo} · Doc. {r.documento}
              </p>
              <p>
                {r.blocoNome}, unidade {r.unidadeNumero}
              </p>
              <p>Entrada: {formatarHora(r.horaEntrada)}</p>
            </div>
            {(r.demorado ?? horasDesde(r.horaEntrada) >= 12) && (
              <span className="selo selo-vencido">Demorado</span>
            )}
            <button
              type="button"
              className="botao-remover"
              onClick={() => onRegistrarSaida(r)}
              disabled={registrandoSaidaId === r.id}
            >
              {registrandoSaidaId === r.id ? 'Registrando...' : 'Registrar saída'}
            </button>
          </div>
          {erroItem?.id === r.id && <p className="alerta-erro">{erroItem.mensagem}</p>}
        </li>
      ))}
    </ul>
  )
}

function PortariaRegistros({ condominioId }) {
  const formVazio = {
    blocoId: '',
    unidadeId: '',
    nome: '',
    documento: '',
    tipo: '',
    quemAutorizou: '',
    placa: '',
  }

  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [quemEstaDentro, setQuemEstaDentro] = useState([])
  const [carregandoDentro, setCarregandoDentro] = useState(true)
  const [erroDentro, setErroDentro] = useState('')
  const [erroItem, setErroItem] = useState(null)
  const [registrandoSaidaId, setRegistrandoSaidaId] = useState(null)

  const [buscaBlocoId, setBuscaBlocoId] = useState('')
  const [buscaUnidadeId, setBuscaUnidadeId] = useState('')
  const [unidadesBusca, setUnidadesBusca] = useState([])
  const [resultadoBusca, setResultadoBusca] = useState(null)
  const [carregandoBusca, setCarregandoBusca] = useState(false)
  const [erroBusca, setErroBusca] = useState('')
  const [erroItemBusca, setErroItemBusca] = useState(null)
  const [registrandoSaidaBuscaId, setRegistrandoSaidaBuscaId] = useState(null)

  const filtroHistoricoVazio = { inicio: '', fim: '', blocoId: '', unidadeId: '', tipo: '' }
  const [filtroHistorico, setFiltroHistorico] = useState(filtroHistoricoVazio)
  const [unidadesHistorico, setUnidadesHistorico] = useState([])
  const [historico, setHistorico] = useState([])
  const [carregandoHistorico, setCarregandoHistorico] = useState(false)
  const [erroHistorico, setErroHistorico] = useState('')
  const [buscouHistorico, setBuscouHistorico] = useState(false)

  useEffect(() => {
    listarBlocos(condominioId)
      .then(setBlocos)
      .catch(() => setBlocos([]))
  }, [condominioId])

  useEffect(() => {
    if (!form.blocoId) {
      setUnidades([])
      return
    }
    listarUnidades(condominioId, form.blocoId)
      .then(setUnidades)
      .catch(() => setUnidades([]))
  }, [condominioId, form.blocoId])

  useEffect(() => {
    if (!buscaBlocoId) {
      setUnidadesBusca([])
      return
    }
    listarUnidades(condominioId, buscaBlocoId)
      .then(setUnidadesBusca)
      .catch(() => setUnidadesBusca([]))
  }, [condominioId, buscaBlocoId])

  useEffect(() => {
    if (!filtroHistorico.blocoId) {
      setUnidadesHistorico([])
      return
    }
    listarUnidades(condominioId, filtroHistorico.blocoId)
      .then(setUnidadesHistorico)
      .catch(() => setUnidadesHistorico([]))
  }, [condominioId, filtroHistorico.blocoId])

  const carregarQuemEstaDentro = useCallback(async () => {
    setCarregandoDentro(true)
    setErroDentro('')
    try {
      setQuemEstaDentro(await listarQuemEstaDentro(condominioId))
    } catch (e) {
      setErroDentro(e.message)
    } finally {
      setCarregandoDentro(false)
    }
  }, [condominioId])

  useEffect(() => {
    carregarQuemEstaDentro()
  }, [carregarQuemEstaDentro])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => {
      if (name === 'blocoId') {
        return { ...anterior, blocoId: value, unidadeId: '' }
      }
      return { ...anterior, [name]: value }
    })
    setErroForm('')
  }

  async function registrarEntrada(evento) {
    evento.preventDefault()
    setErroForm('')

    if (!form.unidadeId) {
      setErroForm('Selecione a unidade.')
      return
    }
    if (!form.quemAutorizou.trim()) {
      setErroForm('Sem autorização prévia, registre quem, na unidade, liberou a entrada.')
      return
    }

    setSalvando(true)
    try {
      await registrarEntradaManual(condominioId, {
        unidadeId: Number(form.unidadeId),
        nome: form.nome.trim(),
        documento: form.documento.trim(),
        tipo: form.tipo.trim(),
        quemAutorizou: form.quemAutorizou.trim(),
        placa: form.placa.trim() || null,
      })
      setForm(formVazio)
      await carregarQuemEstaDentro()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function registrarSaida(registro) {
    setErroItem(null)
    setRegistrandoSaidaId(registro.id)
    try {
      await registrarSaidaAcesso(condominioId, registro.id)
      await carregarQuemEstaDentro()
      if (resultadoBusca) {
        await buscarQuemEstaDentroNaUnidade()
      }
    } catch (e) {
      setErroItem({ id: registro.id, mensagem: e.message })
    } finally {
      setRegistrandoSaidaId(null)
    }
  }

  function atualizarCampoBusca(evento) {
    const { name, value } = evento.target
    if (name === 'buscaBlocoId') {
      setBuscaBlocoId(value)
      setBuscaUnidadeId('')
    } else {
      setBuscaUnidadeId(value)
    }
    setResultadoBusca(null)
    setErroBusca('')
  }

  async function buscarQuemEstaDentroNaUnidade(evento) {
    if (evento) evento.preventDefault()
    if (!buscaUnidadeId) {
      setErroBusca('Selecione a unidade para buscar.')
      return
    }
    setCarregandoBusca(true)
    setErroBusca('')
    try {
      setResultadoBusca(await listarQuemEstaDentroNaUnidade(condominioId, buscaUnidadeId))
    } catch (e) {
      setErroBusca(e.message)
      setResultadoBusca(null)
    } finally {
      setCarregandoBusca(false)
    }
  }

  async function registrarSaidaBusca(registro) {
    setErroItemBusca(null)
    setRegistrandoSaidaBuscaId(registro.id)
    try {
      await registrarSaidaAcesso(condominioId, registro.id)
      await buscarQuemEstaDentroNaUnidade()
      await carregarQuemEstaDentro()
    } catch (e) {
      setErroItemBusca({ id: registro.id, mensagem: e.message })
    } finally {
      setRegistrandoSaidaBuscaId(null)
    }
  }

  function atualizarCampoFiltro(evento) {
    const { name, value } = evento.target
    setFiltroHistorico((anterior) => {
      if (name === 'blocoId') {
        return { ...anterior, blocoId: value, unidadeId: '' }
      }
      return { ...anterior, [name]: value }
    })
  }

  async function filtrarHistorico(evento) {
    if (evento) evento.preventDefault()
    setCarregandoHistorico(true)
    setErroHistorico('')
    setBuscouHistorico(true)
    try {
      setHistorico(
        await filtrarHistoricoAcessos(condominioId, {
          inicio: filtroHistorico.inicio || null,
          fim: filtroHistorico.fim || null,
          unidadeId: filtroHistorico.unidadeId || null,
          tipo: filtroHistorico.tipo || null,
        })
      )
    } catch (e) {
      setErroHistorico(e.message)
      setHistorico([])
    } finally {
      setCarregandoHistorico(false)
    }
  }

  function limparFiltroHistorico() {
    setFiltroHistorico(filtroHistoricoVazio)
    setHistorico([])
    setErroHistorico('')
    setBuscouHistorico(false)
  }

  const colunasHistorico = [
    { chave: 'nome', titulo: 'Nome' },
    { chave: 'documento', titulo: 'Documento' },
    { chave: 'tipo', titulo: 'Tipo' },
    {
      chave: 'unidadeNumero',
      titulo: 'Unidade',
      renderizar: (r) => `${r.blocoNome}, ${r.unidadeNumero}`,
    },
    { chave: 'horaEntrada', titulo: 'Entrada', renderizar: (r) => formatarHora(r.horaEntrada) },
    { chave: 'porteiroNome', titulo: 'Porteiro (entrada)' },
    {
      chave: 'horaSaida',
      titulo: 'Saída',
      renderizar: (r) => (r.horaSaida ? formatarHora(r.horaSaida) : '—'),
    },
    {
      chave: 'porteiroSaidaNome',
      titulo: 'Porteiro (saída)',
      renderizar: (r) => r.porteiroSaidaNome || '—',
    },
    {
      chave: 'demorado',
      titulo: 'Situação',
      renderizar: (r) =>
        r.horaSaida ? (
          'Encerrado'
        ) : r.demorado ? (
          <span className="selo selo-vencido">Demorado</span>
        ) : (
          'Em andamento'
        ),
    },
  ]

  return (
    <section>
      <h2>Registro de entrada</h2>
      <p className="descricao">
        Registre manualmente a entrada de quem não possui autorização prévia cadastrada.
      </p>

      <form onSubmit={registrarEntrada} className="formulario-inline">
        <label className="campo">
          <span>Bloco</span>
          <select name="blocoId" value={form.blocoId} onChange={atualizarCampo} required>
            <option value="">Selecione</option>
            {blocos.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Unidade</span>
          <select
            name="unidadeId"
            value={form.unidadeId}
            onChange={atualizarCampo}
            disabled={!form.blocoId}
            required
          >
            <option value="">Selecione</option>
            {unidades.map((u) => (
              <option key={u.id} value={u.id}>
                {u.numero}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Nome</span>
          <input name="nome" value={form.nome} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Documento</span>
          <input name="documento" value={form.documento} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Tipo</span>
          <input
            name="tipo"
            list="tipos-registro"
            value={form.tipo}
            onChange={atualizarCampo}
            placeholder="Ex: Visita"
            required
          />
          <datalist id="tipos-registro">
            {TIPOS_REGISTRO.map((t) => (
              <option key={t} value={t} />
            ))}
          </datalist>
        </label>
        <label className="campo">
          <span>Placa (opcional)</span>
          <input name="placa" value={form.placa} onChange={atualizarCampo} />
        </label>
        <label className="campo">
          <span>Quem autorizou</span>
          <input
            name="quemAutorizou"
            value={form.quemAutorizou}
            onChange={atualizarCampo}
            placeholder="Nome de quem, na unidade, liberou a entrada"
          />
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          {salvando ? 'Registrando...' : 'Registrar entrada'}
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}

      <h3 className="subtitulo-secao">Quem está dentro agora</h3>
      {erroDentro && <p className="alerta-erro">{erroDentro}</p>}
      {carregandoDentro ? (
        <p className="aviso-vazio">Carregando...</p>
      ) : (
        <ListaDeQuemEstaDentro
          registros={quemEstaDentro}
          onRegistrarSaida={registrarSaida}
          registrandoSaidaId={registrandoSaidaId}
          erroItem={erroItem}
        />
      )}

      <h3 className="subtitulo-secao">Buscar quem está dentro por unidade</h3>
      <p className="descricao">
        Veja rapidamente quem está dentro do condomínio em uma unidade específica.
      </p>
      <form onSubmit={buscarQuemEstaDentroNaUnidade} className="formulario-inline">
        <label className="campo">
          <span>Bloco para busca</span>
          <select name="buscaBlocoId" value={buscaBlocoId} onChange={atualizarCampoBusca}>
            <option value="">Selecione</option>
            {blocos.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Unidade para busca</span>
          <select
            name="buscaUnidadeId"
            value={buscaUnidadeId}
            onChange={atualizarCampoBusca}
            disabled={!buscaBlocoId}
          >
            <option value="">Selecione</option>
            {unidadesBusca.map((u) => (
              <option key={u.id} value={u.id}>
                {u.numero}
              </option>
            ))}
          </select>
        </label>
        <button type="submit" className="botao-secundario" disabled={carregandoBusca}>
          {carregandoBusca ? 'Buscando...' : 'Buscar'}
        </button>
      </form>
      {erroBusca && <p className="alerta-erro">{erroBusca}</p>}
      {resultadoBusca && (
        <ListaDeQuemEstaDentro
          registros={resultadoBusca}
          onRegistrarSaida={registrarSaidaBusca}
          registrandoSaidaId={registrandoSaidaBuscaId}
          erroItem={erroItemBusca}
        />
      )}

      <h3 className="subtitulo-secao">Histórico de acessos</h3>
      <p className="descricao">
        Filtre o histórico completo de entradas e saídas por período, unidade e tipo.
      </p>
      <form onSubmit={filtrarHistorico} className="formulario-inline">
        <label className="campo">
          <span>De</span>
          <input type="date" name="inicio" value={filtroHistorico.inicio} onChange={atualizarCampoFiltro} />
        </label>
        <label className="campo">
          <span>Até</span>
          <input type="date" name="fim" value={filtroHistorico.fim} onChange={atualizarCampoFiltro} />
        </label>
        <label className="campo">
          <span>Filtrar por bloco</span>
          <select name="blocoId" value={filtroHistorico.blocoId} onChange={atualizarCampoFiltro}>
            <option value="">Todos</option>
            {blocos.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Filtrar por unidade</span>
          <select
            name="unidadeId"
            value={filtroHistorico.unidadeId}
            onChange={atualizarCampoFiltro}
            disabled={!filtroHistorico.blocoId}
          >
            <option value="">Todas</option>
            {unidadesHistorico.map((u) => (
              <option key={u.id} value={u.id}>
                {u.numero}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Filtrar por tipo</span>
          <input
            name="tipo"
            list="tipos-registro"
            value={filtroHistorico.tipo}
            onChange={atualizarCampoFiltro}
            placeholder="Todos"
          />
        </label>
        <button type="submit" className="botao-primario" disabled={carregandoHistorico}>
          {carregandoHistorico ? 'Filtrando...' : 'Filtrar'}
        </button>
        <button type="button" className="botao-secundario" onClick={limparFiltroHistorico}>
          Limpar
        </button>
      </form>

      {buscouHistorico && (
        <TabelaDados
          colunas={colunasHistorico}
          dados={historico}
          camposBusca={['nome', 'documento', 'tipo']}
          buscaPlaceholder="Buscar por nome, documento ou tipo..."
          mensagemVazia="Nenhum registro encontrado para os filtros selecionados."
          mensagemErro={erroHistorico}
        />
      )}
    </section>
  )
}

export default PortariaRegistros
