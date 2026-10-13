import { useState, useEffect, useCallback } from 'react'
import {
  atribuirProprietario,
  registrarOcupacao,
  buscarOcupacaoAtual,
  buscarOcupacaoEmData,
  listarOcupacoes,
  listarUsuariosDoCondominio,
} from '../services/api.js'

const TIPOS_OCUPACAO = [
  { valor: 'PROPRIETARIO_MORANDO', rotulo: 'Proprietário morando' },
  { valor: 'ALUGADA', rotulo: 'Alugada' },
  { valor: 'VAZIA', rotulo: 'Vazia' },
]

function rotuloDoTipo(tipo) {
  return TIPOS_OCUPACAO.find((t) => t.valor === tipo)?.rotulo ?? 'Sem ocupação registrada'
}

function formatarData(data) {
  return data.split('-').reverse().join('/')
}

function hoje() {
  const d = new Date()
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

function DetalheUnidade({ condominioId, unidade, nomeBloco, aoAtualizar }) {
  const [ocupacaoAtual, setOcupacaoAtual] = useState(null)
  const [historico, setHistorico] = useState([])
  const [usuarios, setUsuarios] = useState([])
  const [avisoUsuarios, setAvisoUsuarios] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')
  const [erroProprietario, setErroProprietario] = useState('')
  const [erroOcupacao, setErroOcupacao] = useState('')
  const [erroConsulta, setErroConsulta] = useState('')
  const [proprietarioId, setProprietarioId] = useState(unidade.proprietarioId ?? '')
  const [formOcupacao, setFormOcupacao] = useState({
    tipo: 'ALUGADA',
    ocupanteId: '',
    dataEntrada: hoje(),
  })
  const [dataConsulta, setDataConsulta] = useState('')
  const [resultadoConsulta, setResultadoConsulta] = useState(undefined)

  const carregar = useCallback(async () => {
    try {
      const [atual, lista] = await Promise.all([
        buscarOcupacaoAtual(condominioId, unidade.blocoId, unidade.id),
        listarOcupacoes(condominioId, unidade.blocoId, unidade.id),
      ])
      setOcupacaoAtual(atual)
      setHistorico(lista)
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId, unidade.blocoId, unidade.id])

  useEffect(() => {
    carregar()
  }, [carregar])

  useEffect(() => {
    listarUsuariosDoCondominio(condominioId)
      .then((lista) => {
        setUsuarios(lista)
        setAvisoUsuarios('')
      })
      .catch(() =>
        setAvisoUsuarios(
          'Não foi possível carregar a lista de usuários do condomínio. Sem ela não dá para escolher proprietário nem ocupante.'
        )
      )
  }, [condominioId])

  useEffect(() => {
    setProprietarioId(unidade.proprietarioId ?? '')
  }, [unidade.proprietarioId])

  const proprietarios = usuarios.filter((u) => u.perfil === 'PROPRIETARIO')
  const moradores = usuarios.filter((u) => u.perfil === 'MORADOR')
  const opcoesOcupante =
    formOcupacao.tipo === 'PROPRIETARIO_MORANDO' ? proprietarios : moradores

  const nomeProprietario = unidade.proprietarioId
    ? usuarios.find((u) => u.id === unidade.proprietarioId)?.nome ??
      `Usuário nº ${unidade.proprietarioId}`
    : null

  const tipoAtual = ocupacaoAtual?.tipo
  let respondePelaTaxa = nomeProprietario ?? 'Proprietário não definido'
  if (tipoAtual === 'ALUGADA') respondePelaTaxa = ocupacaoAtual.ocupanteNome
  const quemMora = tipoAtual === 'VAZIA' ? 'Ninguém' : ocupacaoAtual?.ocupanteNome

  async function salvarProprietario(evento) {
    evento.preventDefault()
    setErroProprietario('')
    setSalvando(true)
    try {
      await atribuirProprietario(
        condominioId,
        unidade.blocoId,
        unidade.id,
        proprietarioId === '' ? null : Number(proprietarioId)
      )
      await aoAtualizar()
    } catch (e) {
      setErroProprietario(e.message)
    } finally {
      setSalvando(false)
    }
  }

  function atualizarOcupacao(evento) {
    const { name, value } = evento.target
    setFormOcupacao((anterior) => {
      if (name === 'tipo') {
        return { ...anterior, tipo: value, ocupanteId: '' }
      }
      return { ...anterior, [name]: value }
    })
    setErroOcupacao('')
  }

  async function registrar(evento) {
    evento.preventDefault()
    setErroOcupacao('')

    const precisaOcupante = formOcupacao.tipo !== 'VAZIA'
    if (precisaOcupante && !formOcupacao.ocupanteId) {
      setErroOcupacao('Selecione o ocupante.')
      return
    }

    setSalvando(true)
    try {
      await registrarOcupacao(condominioId, unidade.blocoId, unidade.id, {
        tipo: formOcupacao.tipo,
        ocupanteId: precisaOcupante ? Number(formOcupacao.ocupanteId) : null,
        dataEntrada: formOcupacao.dataEntrada,
      })
      setFormOcupacao((anterior) => ({ ...anterior, ocupanteId: '' }))
      await carregar()
      await aoAtualizar()
    } catch (e) {
      setErroOcupacao(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function consultar(evento) {
    evento.preventDefault()
    if (!dataConsulta) return
    setErroConsulta('')
    try {
      const resultado = await buscarOcupacaoEmData(
        condominioId,
        unidade.blocoId,
        unidade.id,
        dataConsulta
      )
      setResultadoConsulta(resultado)
    } catch (e) {
      setErroConsulta(e.message)
    }
  }

  if (carregando) {
    return (
      <div className="painel-unidade">
        <p className="aviso-vazio">Carregando...</p>
      </div>
    )
  }

  return (
    <div className="painel-unidade">
      {erro && <p className="alerta-erro">{erro}</p>}
      {avisoUsuarios && <p className="aviso-vazio">{avisoUsuarios}</p>}

      <div className="secao-unidade">
        <h4>
          Situação atual — {nomeBloco}, unidade {unidade.numero}
        </h4>
        <div className="papeis">
          <div className="papel">
            <span className="papel-rotulo">Situação</span>
            <strong>{rotuloDoTipo(tipoAtual)}</strong>
          </div>
          <div className="papel">
            <span className="papel-rotulo">Proprietário</span>
            <strong>{nomeProprietario ?? 'Não definido'}</strong>
          </div>
          {tipoAtual === 'ALUGADA' && (
            <div className="papel">
              <span className="papel-rotulo">Inquilino</span>
              <strong>{ocupacaoAtual.ocupanteNome}</strong>
            </div>
          )}
          {ocupacaoAtual && (
            <>
              <div className="papel">
                <span className="papel-rotulo">Responde pela taxa ordinária</span>
                <strong>{respondePelaTaxa}</strong>
              </div>
              <div className="papel">
                <span className="papel-rotulo">Mora e autoriza</span>
                <strong>{quemMora}</strong>
              </div>
            </>
          )}
        </div>
      </div>

      <div className="secao-unidade">
        <h4>Proprietário da unidade</h4>
        <form onSubmit={salvarProprietario} className="formulario-inline">
          <label className="campo">
            <span>Proprietário</span>
            <select
              value={proprietarioId}
              onChange={(e) => {
                setProprietarioId(e.target.value)
                setErroProprietario('')
              }}
            >
              <option value="">Sem proprietário</option>
              {proprietarios.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="botao-primario" disabled={salvando}>
            Salvar proprietário
          </button>
        </form>
        {erroProprietario && <p className="alerta-erro">{erroProprietario}</p>}
      </div>

      <div className="secao-unidade">
        <h4>Registrar ocupação</h4>
        <p className="descricao-curta">
          A data de entrada precisa ser posterior à última ocupação. A
          anterior é encerrada automaticamente e nada é apagado.
        </p>
        <form onSubmit={registrar} className="formulario-inline">
          <label className="campo">
            <span>Tipo</span>
            <select name="tipo" value={formOcupacao.tipo} onChange={atualizarOcupacao}>
              {TIPOS_OCUPACAO.map((t) => (
                <option key={t.valor} value={t.valor}>
                  {t.rotulo}
                </option>
              ))}
            </select>
          </label>

          {formOcupacao.tipo !== 'VAZIA' && (
            <label className="campo">
              <span>
                {formOcupacao.tipo === 'ALUGADA' ? 'Inquilino' : 'Proprietário morando'}
              </span>
              <select
                name="ocupanteId"
                value={formOcupacao.ocupanteId}
                onChange={atualizarOcupacao}
              >
                <option value="">Selecione</option>
                {opcoesOcupante.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.nome}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="campo">
            <span>Data de entrada</span>
            <input
              type="date"
              name="dataEntrada"
              value={formOcupacao.dataEntrada}
              onChange={atualizarOcupacao}
              required
            />
          </label>

          <button type="submit" className="botao-primario" disabled={salvando}>
            Registrar ocupação
          </button>
        </form>
        {erroOcupacao && <p className="alerta-erro">{erroOcupacao}</p>}
      </div>

      <div className="secao-unidade">
        <h4>Linha do tempo</h4>
        {historico.length === 0 ? (
          <p className="aviso-vazio">Nenhuma ocupação registrada ainda.</p>
        ) : (
          <ol className="linha-do-tempo">
            {historico.map((o, i) => {
              const fim = i === 0 ? null : historico[i - 1].dataEntrada
              return (
                <li key={o.id} className={i === 0 ? 'item-tempo atual' : 'item-tempo'}>
                  <strong>{rotuloDoTipo(o.tipo)}</strong>
                  <span>{o.ocupanteNome ?? 'Ninguém'}</span>
                  <span className="periodo">
                    {formatarData(o.dataEntrada)}{' '}
                    {fim ? `até ${formatarData(fim)}` : '· em andamento'}
                  </span>
                </li>
              )
            })}
          </ol>
        )}
      </div>

      <div className="secao-unidade">
        <h4>Quem ocupava em uma data</h4>
        <form onSubmit={consultar} className="formulario-inline">
          <label className="campo">
            <span>Data</span>
            <input
              type="date"
              value={dataConsulta}
              onChange={(e) => {
                setDataConsulta(e.target.value)
                setResultadoConsulta(undefined)
                setErroConsulta('')
              }}
              required
            />
          </label>
          <button type="submit" className="botao-primario">
            Consultar
          </button>
        </form>

        {erroConsulta && <p className="alerta-erro">{erroConsulta}</p>}
        {resultadoConsulta === null && (
          <p className="aviso-vazio resultado-consulta">Nenhum registro cobre essa data.</p>
        )}
        {resultadoConsulta && (
          <p className="resultado-consulta">
            Em {formatarData(dataConsulta)}: <strong>{rotuloDoTipo(resultadoConsulta.tipo)}</strong>
            {resultadoConsulta.ocupanteNome ? ` — ${resultadoConsulta.ocupanteNome}` : ''}
          </p>
        )}
      </div>
    </div>
  )
}

export default DetalheUnidade