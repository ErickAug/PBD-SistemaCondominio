import { useEffect, useState, useCallback } from 'react'
import { buscarVisitasEsperadas, listarVisitasEsperadasHoje } from '../services/api.js'

function formatarData(data) {
  return data.split('-').reverse().join('/')
}

const ROTULOS_SITUACAO = {
  LIBERADA: 'Liberada',
  AGUARDANDO: 'Aguardando',
  VENCIDA: 'Vencida',
  USADA: 'Usada',
  CANCELADA: 'Cancelada',
}

const ROTULOS_ORIGEM = {
  AVULSA: 'Visita avulsa',
  PERMANENTE: 'Autorizado permanente',
}

function seloSituacao(situacao) {
  if (situacao === 'LIBERADA') return 'selo selo-ativo'
  if (situacao === 'VENCIDA') return 'selo selo-vencido'
  return 'selo selo-inativo'
}

function ItemVisita({ visita }) {
  return (
    <li className="item-unidade">
      <div className="item-condominio">
        <div>
          <strong>{visita.nome}</strong>
          <p>
            {visita.tipo} · Doc. {visita.documento}
          </p>
          <p>
            {visita.blocoNome}, unidade {visita.unidadeNumero}
          </p>
          <p>
            {visita.dataInicio === visita.dataFim
              ? formatarData(visita.dataInicio)
              : `${formatarData(visita.dataInicio)} a ${formatarData(visita.dataFim)}`}
            {' · '}
            {ROTULOS_ORIGEM[visita.origem] ?? visita.origem}
          </p>
        </div>
        <span className={seloSituacao(visita.situacao)}>
          {ROTULOS_SITUACAO[visita.situacao] ?? visita.situacao}
        </span>
      </div>
    </li>
  )
}

function PortariaVisitas({ condominioId }) {
  const [termo, setTermo] = useState('')
  const [termoBuscado, setTermoBuscado] = useState('')
  const [resultadoBusca, setResultadoBusca] = useState(null)
  const [buscando, setBuscando] = useState(false)
  const [erroBusca, setErroBusca] = useState('')

  const [esperadasHoje, setEsperadasHoje] = useState([])
  const [carregandoHoje, setCarregandoHoje] = useState(true)
  const [erroHoje, setErroHoje] = useState('')

  const carregarHoje = useCallback(async () => {
    setCarregandoHoje(true)
    setErroHoje('')
    try {
      setEsperadasHoje(await listarVisitasEsperadasHoje(condominioId))
    } catch (e) {
      setErroHoje(e.message)
    } finally {
      setCarregandoHoje(false)
    }
  }, [condominioId])

  useEffect(() => {
    carregarHoje()
  }, [carregarHoje])

  async function buscar(evento) {
    evento.preventDefault()
    setErroBusca('')
    setBuscando(true)
    try {
      const termoAtual = termo.trim()
      const encontradas = await buscarVisitasEsperadas(condominioId, termoAtual)
      setTermoBuscado(termoAtual)
      setResultadoBusca(encontradas)
    } catch (e) {
      setResultadoBusca(null)
      setErroBusca(e.message)
    } finally {
      setBuscando(false)
    }
  }

  return (
    <section>
      <h2>Visitas</h2>
      <p className="descricao">
        Busque por nome ou documento para liberar visitas, prestadores de serviço e entregas
        autorizados pelas unidades.
      </p>

      <form onSubmit={buscar} className="formulario-inline">
        <label className="campo">
          <span>Nome ou documento</span>
          <input
            value={termo}
            onChange={(e) => {
              setTermo(e.target.value)
              setErroBusca('')
            }}
            placeholder="Ex: João ou 123.456.789-00"
            required
            autoFocus
          />
        </label>
        <button type="submit" className="botao-primario" disabled={buscando}>
          {buscando ? 'Buscando...' : 'Buscar'}
        </button>
      </form>

      {erroBusca && <p className="alerta-erro">{erroBusca}</p>}

      {resultadoBusca && (
        <>
          <h5 className="subtitulo-lista">
            {resultadoBusca.length === 0
              ? `Nada encontrado para "${termoBuscado}"`
              : `${resultadoBusca.length} resultado(s) para "${termoBuscado}"`}
          </h5>
          <ul className="lista-condominios">
            {resultadoBusca.map((v) => (
              <ItemVisita key={`${v.origem}-${v.id}`} visita={v} />
            ))}
          </ul>
        </>
      )}

      <h3 className="subtitulo-secao">Visitas esperadas hoje</h3>

      {erroHoje && <p className="alerta-erro">{erroHoje}</p>}

      {carregandoHoje ? (
        <p className="aviso-vazio">Carregando...</p>
      ) : esperadasHoje.length === 0 ? (
        <p className="aviso-vazio">Nenhuma visita esperada hoje.</p>
      ) : (
        <ul className="lista-condominios">
          {esperadasHoje.map((v) => (
            <ItemVisita key={`${v.origem}-${v.id}`} visita={v} />
          ))}
        </ul>
      )}
    </section>
  )
}

export default PortariaVisitas
