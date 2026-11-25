import { useState, useEffect, useCallback } from 'react'
import {
  listarAreasComuns,
  criarReserva,
  listarReservasDaUnidade,
  cancelarReserva,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import CalendarioReservas from './CalendarioReservas.jsx'

const SITUACOES = {
  PENDENTE: { rotulo: 'Pendente', classe: 'selo-inativo' },
  CONFIRMADA: { rotulo: 'Confirmada', classe: 'selo-ativo' },
  RECUSADA: { rotulo: 'Recusada', classe: 'selo-vencido' },
  CANCELADA: { rotulo: 'Cancelada', classe: 'selo-vencido' },
}

const SITUACOES_ATIVAS = ['PENDENTE', 'CONFIRMADA']

function seloSituacao(situacao) {
  const info = SITUACOES[situacao] ?? { rotulo: situacao, classe: 'selo-inativo' }
  return <span className={`selo ${info.classe}`}>{info.rotulo}</span>
}

function formatarData(data) {
  if (!data) return ''
  return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR')
}

function formatarHora(hora) {
  if (!hora) return ''
  return hora.slice(0, 5)
}

function hoje() {
  return new Date().toISOString().slice(0, 10)
}

function podeCancelar(reserva) {
  return SITUACOES_ATIVAS.includes(reserva.situacao) && reserva.data > hoje()
}

function MinhasReservas({ condominioId, blocoId, unidadeId, podeUsar = true }) {
  const formVazio = { areaComumId: '', data: '', horaInicio: '', horaFim: '', finalidade: '' }

  const [areas, setAreas] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [reservas, setReservas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroLista, setErroLista] = useState('')

  const [cancelandoId, setCancelandoId] = useState(null)
  const [erroCancelar, setErroCancelar] = useState(null)
  const [refreshKey, setRefreshKey] = useState(0)

  useEffect(() => {
    listarAreasComuns(condominioId).then(setAreas).catch(() => setAreas([]))
  }, [condominioId])

  const carregar = useCallback(() => {
    if (!blocoId || !unidadeId || !podeUsar) {
      setCarregando(false)
      return
    }
    setCarregando(true)
    setErroLista('')
    listarReservasDaUnidade(condominioId, blocoId, unidadeId)
      .then(setReservas)
      .catch((e) => { setErroLista(e.message); setReservas([]) })
      .finally(() => setCarregando(false))
  }, [condominioId, blocoId, unidadeId, podeUsar])

  useEffect(() => {
    carregar()
  }, [carregar])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErroForm('')
  }

  async function reservar(evento) {
    evento.preventDefault()
    setErroForm('')
    if (!form.areaComumId) { setErroForm('Selecione a área comum.'); return }
    if (!form.data) { setErroForm('Informe a data da reserva.'); return }
    if (!form.horaInicio || !form.horaFim) { setErroForm('Informe o horário de início e de fim.'); return }
    if (!form.finalidade.trim()) { setErroForm('Informe a finalidade da reserva.'); return }

    setSalvando(true)
    try {
      await criarReserva(condominioId, blocoId, unidadeId, {
        areaComumId: Number(form.areaComumId),
        data: form.data,
        horaInicio: form.horaInicio,
        horaFim: form.horaFim,
        finalidade: form.finalidade.trim(),
      })
      setForm(formVazio)
      carregar()
      setRefreshKey((anterior) => anterior + 1)
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function cancelar(reserva) {
    setErroCancelar(null)
    setCancelandoId(reserva.id)
    try {
      await cancelarReserva(condominioId, blocoId, unidadeId, reserva.id)
      carregar()
      setRefreshKey((anterior) => anterior + 1)
    } catch (e) {
      setErroCancelar({ id: reserva.id, mensagem: e.message })
    } finally {
      setCancelandoId(null)
    }
  }

  const colunas = [
    { chave: 'areaComumNome', titulo: 'Área' },
    { chave: 'data', titulo: 'Data', renderizar: (r) => formatarData(r.data) },
    {
      chave: 'horaInicio', titulo: 'Horário',
      renderizar: (r) => `${formatarHora(r.horaInicio)}–${formatarHora(r.horaFim)}`,
    },
    { chave: 'finalidade', titulo: 'Finalidade' },
    {
      chave: 'situacao', titulo: 'Situação', ordenavel: false,
      renderizar: (r) => (
        <div>
          {seloSituacao(r.situacao)}
          {r.situacao === 'RECUSADA' && r.motivoRecusa && (
            <p className="descricao-curta">{r.motivoRecusa}</p>
          )}
        </div>
      ),
    },
    {
      chave: 'acoes', titulo: 'Ações', ordenavel: false,
      renderizar: (r) =>
        podeCancelar(r) ? (
          <button type="button" className="botao-secundario" onClick={() => cancelar(r)} disabled={cancelandoId === r.id}>
            {cancelandoId === r.id ? 'Cancelando...' : 'Cancelar'}
          </button>
        ) : null,
    },
  ]

  if (!blocoId || !unidadeId) {
    return (
      <section>
        <h2>Reservar área comum</h2>
        <p className="aviso-vazio">
          Não foi possível identificar a sua unidade. Se você acabou de ser
          cadastrado, peça ao síndico para registrar a sua entrada.
        </p>
      </section>
    )
  }

  return (
    <section>
      <h2>Reservar área comum</h2>
      <p className="descricao">
        Reserve o salão ou outras áreas comuns com antecedência. Duas reservas
        do mesmo espaço no mesmo horário não coexistem, e a unidade precisa
        estar em dia com as cobranças para reservar.
      </p>

      {podeUsar ? (
        <>
          <h3 className="subtitulo-secao">Nova reserva</h3>
          <form onSubmit={reservar} className="formulario-inline">
            <label className="campo">
              <span>Área comum</span>
              <select name="areaComumId" value={form.areaComumId} onChange={atualizarCampo}>
                <option value="">Selecione</option>
                {areas.map((a) => (<option key={a.id} value={a.id}>{a.nome}</option>))}
              </select>
            </label>
            <label className="campo">
              <span>Data</span>
              <input type="date" name="data" value={form.data} onChange={atualizarCampo} min={hoje()} />
            </label>
            <label className="campo">
              <span>Início</span>
              <input type="time" name="horaInicio" value={form.horaInicio} onChange={atualizarCampo} />
            </label>
            <label className="campo">
              <span>Fim</span>
              <input type="time" name="horaFim" value={form.horaFim} onChange={atualizarCampo} />
            </label>
            <label className="campo campo-descricao-ocorrencia">
              <span>Finalidade</span>
              <input name="finalidade" value={form.finalidade} onChange={atualizarCampo} placeholder="Ex: Aniversário de 10 anos" />
            </label>
            <button type="submit" className="botao-primario" disabled={salvando}>
              {salvando ? 'Reservando...' : 'Reservar'}
            </button>
          </form>
          {erroForm && <p className="alerta-erro">{erroForm}</p>}

          <h3 className="subtitulo-secao">Minhas reservas</h3>
          {erroCancelar && <p className="alerta-erro">{erroCancelar.mensagem}</p>}
          <TabelaDados
            colunas={colunas}
            dados={reservas}
            camposBusca={['areaComumNome', 'finalidade']}
            buscaPlaceholder="Buscar por área ou finalidade..."
            mensagemVazia={carregando ? 'Carregando...' : 'Nenhuma reserva ainda.'}
            mensagemErro={erroLista}
          />
        </>
      ) : (
        <p className="aviso-vazio">
          Você não mora nesta unidade, então não pode reservar áreas comuns por ela.
        </p>
      )}

      <h3 className="subtitulo-secao">Calendário de reservas</h3>
      <CalendarioReservas condominioId={condominioId} refreshKey={refreshKey} />
    </section>
  )
}

export default MinhasReservas
