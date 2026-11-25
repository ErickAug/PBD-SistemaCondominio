import { useState, useEffect, useCallback } from 'react'
import {
  listarBlocos,
  listarUnidades,
  registrarOcorrencia,
  corrigirOcorrencia,
  resolverOcorrencia,
  filtrarOcorrencias,
  encerrarTurno,
  buscarUltimaPassagemTurno,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

const TIPOS_OCORRENCIA = [
  'Barulho',
  'Portão com defeito',
  'Veículo irregular',
  'Entrega deixada na portaria',
  'Outro',
]

function formatarDataHora(dataHora) {
  if (!dataHora) return ''
  return new Date(dataHora).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function LivroOcorrencias({ condominioId, somenteLeitura = false }) {
  const formVazio = { blocoId: '', unidadeId: '', tipo: '', texto: '' }

  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const filtroVazio = { inicio: '', fim: '', blocoId: '', unidadeId: '', tipo: '' }
  const [filtro, setFiltro] = useState(filtroVazio)
  const [unidadesFiltro, setUnidadesFiltro] = useState([])
  const [ocorrencias, setOcorrencias] = useState([])
  const [carregando, setCarregando] = useState(false)
  const [erroLista, setErroLista] = useState('')

  const [ultimaPassagem, setUltimaPassagem] = useState(null)
  const [carregandoPassagem, setCarregandoPassagem] = useState(!somenteLeitura)

  const [corrigindoId, setCorrigindoId] = useState(null)
  const [textoCorrecao, setTextoCorrecao] = useState('')
  const [salvandoAcaoId, setSalvandoAcaoId] = useState(null)
  const [erroAcao, setErroAcao] = useState(null)

  const [confirmandoEncerramento, setConfirmandoEncerramento] = useState(false)
  const [encerrando, setEncerrando] = useState(false)
  const [erroEncerrar, setErroEncerrar] = useState('')

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
    if (!filtro.blocoId) {
      setUnidadesFiltro([])
      return
    }
    listarUnidades(condominioId, filtro.blocoId)
      .then(setUnidadesFiltro)
      .catch(() => setUnidadesFiltro([]))
  }, [condominioId, filtro.blocoId])

  const carregarUltimaPassagem = useCallback(async () => {
    setCarregandoPassagem(true)
    try {
      setUltimaPassagem(await buscarUltimaPassagemTurno(condominioId))
    } catch {
      setUltimaPassagem(null)
    } finally {
      setCarregandoPassagem(false)
    }
  }, [condominioId])

  useEffect(() => {
    if (!somenteLeitura) carregarUltimaPassagem()
  }, [somenteLeitura, carregarUltimaPassagem])

  const aplicarFiltro = useCallback(async () => {
    setCarregando(true)
    setErroLista('')
    try {
      setOcorrencias(
        await filtrarOcorrencias(condominioId, {
          inicio: filtro.inicio || null,
          fim: filtro.fim || null,
          tipo: filtro.tipo || null,
          unidadeId: filtro.unidadeId || null,
        })
      )
    } catch (e) {
      setErroLista(e.message)
      setOcorrencias([])
    } finally {
      setCarregando(false)
    }
  }, [condominioId, filtro])

  useEffect(() => {
    aplicarFiltro()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [condominioId])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => {
      if (name === 'blocoId') return { ...anterior, blocoId: value, unidadeId: '' }
      return { ...anterior, [name]: value }
    })
    setErroForm('')
  }

  async function registrar(evento) {
    evento.preventDefault()
    setErroForm('')

    if (!form.texto.trim()) {
      setErroForm('Descreva o que aconteceu.')
      return
    }
    if (!form.tipo.trim()) {
      setErroForm('Informe o tipo da ocorrência.')
      return
    }

    setSalvando(true)
    try {
      await registrarOcorrencia(condominioId, {
        texto: form.texto.trim(),
        tipo: form.tipo.trim(),
        unidadeId: form.unidadeId ? Number(form.unidadeId) : null,
      })
      setForm(formVazio)
      await aplicarFiltro()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  function atualizarCampoFiltro(evento) {
    const { name, value } = evento.target
    setFiltro((anterior) => {
      if (name === 'blocoId') return { ...anterior, blocoId: value, unidadeId: '' }
      return { ...anterior, [name]: value }
    })
  }

  function filtrar(evento) {
    if (evento) evento.preventDefault()
    aplicarFiltro()
  }

  function limparFiltro() {
    setFiltro(filtroVazio)
    setErroLista('')
    setCarregando(true)
    filtrarOcorrencias(condominioId, {})
      .then(setOcorrencias)
      .catch((e) => {
        setErroLista(e.message)
        setOcorrencias([])
      })
      .finally(() => setCarregando(false))
  }

  async function marcarResolvida(ocorrencia) {
    setErroAcao(null)
    setSalvandoAcaoId(ocorrencia.id)
    try {
      await resolverOcorrencia(condominioId, ocorrencia.id)
      await aplicarFiltro()
      if (!somenteLeitura) await carregarUltimaPassagem()
    } catch (e) {
      setErroAcao({ id: ocorrencia.id, mensagem: e.message })
    } finally {
      setSalvandoAcaoId(null)
    }
  }

  function abrirCorrecao(ocorrencia) {
    setCorrigindoId(ocorrencia.id)
    setTextoCorrecao(ocorrencia.texto)
    setErroAcao(null)
  }

  function cancelarCorrecao() {
    setCorrigindoId(null)
    setTextoCorrecao('')
  }

  async function salvarCorrecao(ocorrencia) {
    if (!textoCorrecao.trim()) {
      setErroAcao({ id: ocorrencia.id, mensagem: 'Descreva a correção.' })
      return
    }

    setErroAcao(null)
    setSalvandoAcaoId(ocorrencia.id)
    try {
      await corrigirOcorrencia(condominioId, ocorrencia.id, textoCorrecao.trim())
      setCorrigindoId(null)
      setTextoCorrecao('')
      await aplicarFiltro()
    } catch (e) {
      setErroAcao({ id: ocorrencia.id, mensagem: e.message })
    } finally {
      setSalvandoAcaoId(null)
    }
  }

  async function confirmarEncerramento() {
    setErroEncerrar('')
    setEncerrando(true)
    try {
      await encerrarTurno(condominioId)
      setConfirmandoEncerramento(false)
      await aplicarFiltro()
      await carregarUltimaPassagem()
    } catch (e) {
      setErroEncerrar(e.message)
    } finally {
      setEncerrando(false)
    }
  }

  const colunas = [
    { chave: 'dataHora', titulo: 'Data/hora', renderizar: (o) => formatarDataHora(o.dataHora) },
    { chave: 'tipo', titulo: 'Tipo' },
    {
      chave: 'unidadeNumero',
      titulo: 'Unidade',
      renderizar: (o) => (o.unidadeId ? `${o.blocoNome}, ${o.unidadeNumero}` : '—'),
    },
    {
      chave: 'texto',
      titulo: 'Anotação',
      ordenavel: false,
      renderizar: (o) => (
        <div>
          <p>{o.texto}</p>
          {o.correcaoDeId && (
            <p className="descricao-curta anotacao-correcao">
              Correção da anotação nº {o.correcaoDeId}
            </p>
          )}
        </div>
      ),
    },
    { chave: 'autorNome', titulo: 'Autor' },
    {
      chave: 'resolvida',
      titulo: 'Situação',
      renderizar: (o) =>
        o.resolvida ? (
          <span className="selo selo-ativo">
            Resolvida
            {o.resolvidoPorNome ? ` · ${o.resolvidoPorNome}` : ''}
            {o.resolvidoEm ? ` em ${formatarDataHora(o.resolvidoEm)}` : ''}
          </span>
        ) : (
          <span className="selo selo-inativo">Pendente</span>
        ),
    },
  ]

  if (!somenteLeitura) {
    colunas.push({
      chave: 'acoes',
      titulo: 'Ações',
      ordenavel: false,
      renderizar: (o) =>
        corrigindoId === o.id ? (
          <div className="formulario-correcao">
            <label className="campo">
              <span>Nova redação</span>
              <textarea
                value={textoCorrecao}
                onChange={(e) => setTextoCorrecao(e.target.value)}
                rows={2}
              />
            </label>
            <div className="tabela-dados-acoes">
              <button
                type="button"
                className="botao-primario"
                onClick={() => salvarCorrecao(o)}
                disabled={salvandoAcaoId === o.id}
              >
                {salvandoAcaoId === o.id ? 'Salvando...' : 'Salvar'}
              </button>
              <button type="button" className="botao-secundario" onClick={cancelarCorrecao}>
                Cancelar
              </button>
            </div>
          </div>
        ) : (
          <div className="tabela-dados-acoes">
            <button type="button" className="botao-secundario" onClick={() => abrirCorrecao(o)}>
              Corrigir
            </button>
            {!o.resolvida && (
              <button
                type="button"
                className="botao-primario"
                onClick={() => marcarResolvida(o)}
                disabled={salvandoAcaoId === o.id}
              >
                {salvandoAcaoId === o.id ? 'Salvando...' : 'Resolver'}
              </button>
            )}
          </div>
        ),
    })
  }

  return (
    <section>
      <h2>Livro de ocorrências</h2>
      <p className="descricao">
        {somenteLeitura
          ? 'Tudo o que a portaria anotou durante os plantões. Anotações não podem ser editadas ou excluídas — uma correção vira uma anotação nova, ligada à anterior.'
          : 'Anote o que aconteceu no plantão. Anotações não podem ser editadas ou excluídas — para corrigir algo, registre uma nova anotação ligada à anterior.'}
      </p>

      <datalist id="tipos-ocorrencia">
        {TIPOS_OCORRENCIA.map((t) => (
          <option key={t} value={t} />
        ))}
      </datalist>

      {!somenteLeitura && !carregandoPassagem && ultimaPassagem && (
        <div className={ultimaPassagem.pendencias.length > 0 ? 'painel-fracao alerta' : 'painel-fracao ok'}>
          <strong>Turno anterior encerrado por {ultimaPassagem.porteiroNome}</strong> em{' '}
          {formatarDataHora(ultimaPassagem.horarioEncerramento)}
          {ultimaPassagem.pendencias.length === 0 ? (
            <p>Nenhuma pendência deixada.</p>
          ) : (
            <>
              <p>{ultimaPassagem.pendencias.length} pendência(s) deixada(s):</p>
              <ul>
                {ultimaPassagem.pendencias.map((p) => (
                  <li key={p.id}>
                    {p.tipo} — {p.texto}
                    {p.unidadeId ? ` (${p.blocoNome}, ${p.unidadeNumero})` : ''}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}

      {!somenteLeitura && (
        <>
          <h3 className="subtitulo-secao">Nova anotação</h3>
          <form onSubmit={registrar} className="formulario-inline">
            <label className="campo">
              <span>Tipo</span>
              <input
                name="tipo"
                list="tipos-ocorrencia"
                value={form.tipo}
                onChange={atualizarCampo}
                placeholder="Ex: Barulho"
                required
              />
            </label>
            <label className="campo">
              <span>Bloco (opcional)</span>
              <select name="blocoId" value={form.blocoId} onChange={atualizarCampo}>
                <option value="">Nenhum</option>
                {blocos.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.nome}
                  </option>
                ))}
              </select>
            </label>
            <label className="campo">
              <span>Unidade (opcional)</span>
              <select
                name="unidadeId"
                value={form.unidadeId}
                onChange={atualizarCampo}
                disabled={!form.blocoId}
              >
                <option value="">Nenhuma</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.numero} (andar {u.andar})
                  </option>
                ))}
              </select>
            </label>
            <label className="campo">
              <span>O que aconteceu</span>
              <input
                name="texto"
                value={form.texto}
                onChange={atualizarCampo}
                placeholder="Ex: Portão da garagem travou ao fechar"
                required
              />
            </label>
            <button type="submit" className="botao-primario" disabled={salvando}>
              {salvando ? 'Registrando...' : 'Registrar anotação'}
            </button>
          </form>
          {erroForm && <p className="alerta-erro">{erroForm}</p>}

          <h3 className="subtitulo-secao">Encerrar turno</h3>
          <p className="descricao-curta">
            Fecha o plantão e registra as ocorrências ainda pendentes para o próximo porteiro ver
            ao entrar.
          </p>
          {!confirmandoEncerramento ? (
            <button type="button" className="botao-secundario" onClick={() => setConfirmandoEncerramento(true)}>
              Encerrar turno
            </button>
          ) : (
            <div className="confirmacao">
              <p>Confirma o encerramento do turno agora?</p>
              <button
                type="button"
                className="botao-primario"
                onClick={confirmarEncerramento}
                disabled={encerrando}
              >
                {encerrando ? 'Encerrando...' : 'Confirmar'}
              </button>{' '}
              <button type="button" className="botao-secundario" onClick={() => setConfirmandoEncerramento(false)}>
                Cancelar
              </button>
            </div>
          )}
          {erroEncerrar && <p className="alerta-erro">{erroEncerrar}</p>}
        </>
      )}

      <h3 className="subtitulo-secao">Consultar o livro</h3>
      <form onSubmit={filtrar} className="formulario-inline">
        <label className="campo">
          <span>De</span>
          <input type="date" name="inicio" value={filtro.inicio} onChange={atualizarCampoFiltro} />
        </label>
        <label className="campo">
          <span>Até</span>
          <input type="date" name="fim" value={filtro.fim} onChange={atualizarCampoFiltro} />
        </label>
        <label className="campo">
          <span>Bloco</span>
          <select name="blocoId" value={filtro.blocoId} onChange={atualizarCampoFiltro}>
            <option value="">Todos</option>
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
            value={filtro.unidadeId}
            onChange={atualizarCampoFiltro}
            disabled={!filtro.blocoId}
          >
            <option value="">Todas</option>
            {unidadesFiltro.map((u) => (
              <option key={u.id} value={u.id}>
                {u.numero} (andar {u.andar})
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Tipo</span>
          <input
            name="tipo"
            list="tipos-ocorrencia"
            value={filtro.tipo}
            onChange={atualizarCampoFiltro}
            placeholder="Todos"
          />
        </label>
        <button type="submit" className="botao-primario" disabled={carregando}>
          {carregando ? 'Filtrando...' : 'Filtrar'}
        </button>
        <button type="button" className="botao-secundario" onClick={limparFiltro}>
          Limpar
        </button>
      </form>

      {erroAcao && <p className="alerta-erro">{erroAcao.mensagem}</p>}

      <TabelaDados
        colunas={colunas}
        dados={ocorrencias}
        camposBusca={['texto', 'tipo', 'autorNome']}
        buscaPlaceholder="Buscar por texto, tipo ou autor..."
        mensagemVazia="Nenhuma ocorrência encontrada para os filtros selecionados."
        mensagemErro={erroLista}
      />
    </section>
  )
}

export default LivroOcorrencias
