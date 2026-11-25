import { useState, useEffect, useCallback } from 'react'
import { listarAreasComuns, buscarCalendarioReservas } from '../services/api.js'

const SITUACOES = {
  PENDENTE: { rotulo: 'Pendente', classe: 'selo-inativo' },
  CONFIRMADA: { rotulo: 'Confirmada', classe: 'selo-ativo' },
  RECUSADA: { rotulo: 'Recusada', classe: 'selo-vencido' },
  CANCELADA: { rotulo: 'Cancelada', classe: 'selo-vencido' },
}

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

function mesAtual() {
  const hoje = new Date()
  return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}`
}

function CalendarioReservas({ condominioId }) {
  const [areas, setAreas] = useState([])
  const [mes, setMes] = useState(mesAtual())
  const [areaComumId, setAreaComumId] = useState('')
  const [calendario, setCalendario] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    listarAreasComuns(condominioId).then(setAreas).catch(() => setAreas([]))
  }, [condominioId])

  const carregar = useCallback(() => {
    setCarregando(true)
    setErro('')
    buscarCalendarioReservas(condominioId, mes, areaComumId || null)
      .then(setCalendario)
      .catch((e) => { setErro(e.message); setCalendario([]) })
      .finally(() => setCarregando(false))
  }, [condominioId, mes, areaComumId])

  useEffect(() => {
    carregar()
  }, [carregar])

  return (
    <div>
      <div className="formulario-inline">
        <label className="campo">
          <span>Mês</span>
          <input type="month" value={mes} onChange={(e) => setMes(e.target.value)} />
        </label>
        <label className="campo">
          <span>Área comum</span>
          <select value={areaComumId} onChange={(e) => setAreaComumId(e.target.value)}>
            <option value="">Todas</option>
            {areas.map((a) => (<option key={a.id} value={a.id}>{a.nome}</option>))}
          </select>
        </label>
      </div>

      {carregando && <p className="descricao-curta">Carregando...</p>}
      {erro && <p className="alerta-erro">{erro}</p>}

      {!carregando && !erro && calendario.map((area) => (
        <div key={area.areaComumId} className="calendario-area">
          <h4>{area.areaComumNome}</h4>
          {area.reservas.length === 0 ? (
            <p className="descricao-curta">Nenhuma reserva neste mês.</p>
          ) : (
            <ul className="lista-leituras-aviso">
              {area.reservas.map((r) => (
                <li key={r.id}>
                  <strong>{formatarData(r.data)}</strong>, {formatarHora(r.horaInicio)}–{formatarHora(r.horaFim)}
                  {' '}— {r.blocoNome}, {r.unidadeNumero} {seloSituacao(r.situacao)}
                  <p className="descricao-curta">{r.finalidade}</p>
                </li>
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  )
}

export default CalendarioReservas
