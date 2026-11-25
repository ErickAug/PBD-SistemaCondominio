import { useState, useEffect, useCallback } from 'react'
import { buscarPainelUnidade } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatarData(data) {
  if (!data) return ''
  return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR')
}

function formatarHora(hora) {
  if (!hora) return ''
  return hora.slice(0, 5)
}

function formatarDataHora(dataHora) {
  if (!dataHora) return ''
  return new Date(dataHora).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

function formatarCompetencia(competencia) {
  if (!competencia) return ''
  const [ano, mes] = competencia.split('-')
  return `${mes}/${ano}`
}

function competenciaPadrao() {
  const hoje = new Date()
  const data = new Date(hoje.getFullYear(), hoje.getMonth() - 1, 1)
  const ano = data.getFullYear()
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  return `${ano}-${mes}`
}

function rotuloAlcance(valor) {
  if (valor === 'CONDOMINIO') return 'Todo o condomínio'
  if (valor === 'BLOCO') return 'Bloco'
  if (valor === 'UNIDADE') return 'Unidade'
  return valor
}

function rotuloPapel(papel) {
  if (papel === 'MORADOR') return 'Morador'
  if (papel === 'PROPRIETARIO') return 'Proprietário'
  return papel
}

function seloCobranca(c) {
  if (c.situacao === 'PAGA') return <span className="selo selo-ativo">Paga</span>
  if (c.emAtraso) return <span className="selo selo-vencido">Em atraso ({c.diasAtraso}d)</span>
  return <span className="selo selo-inativo">Aberta</span>
}

const SITUACOES_RESERVA = {
  PENDENTE: { rotulo: 'Pendente', classe: 'selo-inativo' },
  CONFIRMADA: { rotulo: 'Confirmada', classe: 'selo-ativo' },
  RECUSADA: { rotulo: 'Recusada', classe: 'selo-vencido' },
  CANCELADA: { rotulo: 'Cancelada', classe: 'selo-vencido' },
}

function seloReserva(situacao) {
  const info = SITUACOES_RESERVA[situacao] ?? { rotulo: situacao, classe: 'selo-inativo' }
  return <span className={`selo ${info.classe}`}>{info.rotulo}</span>
}

const SITUACOES_OCORRENCIA = {
  ABERTA: { rotulo: 'Aberta', classe: 'selo-inativo' },
  EM_ANALISE: { rotulo: 'Em análise', classe: 'selo-inativo' },
  RESOLVIDA: { rotulo: 'Resolvida', classe: 'selo-ativo' },
  RECUSADA: { rotulo: 'Recusada', classe: 'selo-vencido' },
}

function seloOcorrencia(situacao) {
  const info = SITUACOES_OCORRENCIA[situacao] ?? { rotulo: situacao, classe: 'selo-inativo' }
  return <span className={`selo ${info.classe}`}>{info.rotulo}</span>
}

const COLUNAS_DESPESAS = [
  { chave: 'categoria', titulo: 'Categoria' },
  { chave: 'valor', titulo: 'Valor', renderizar: (item) => formatarMoeda(item.valor) },
]

const COLUNAS_COBRANCAS = [
  { chave: 'competencia', titulo: 'Competência', renderizar: (c) => formatarCompetencia(c.competencia) },
  { chave: 'vencimento', titulo: 'Vencimento', renderizar: (c) => formatarData(c.vencimento) },
  { chave: 'situacao', titulo: 'Situação', ordenavel: false, renderizar: seloCobranca },
  {
    chave: 'parteOrdinaria', titulo: 'Parte ordinária',
    renderizar: (c) => (c.parteOrdinaria != null ? formatarMoeda(c.parteOrdinaria) : '—'),
  },
  {
    chave: 'parteExtraordinaria', titulo: 'Parte extraordinária',
    renderizar: (c) => (c.parteExtraordinaria != null ? formatarMoeda(c.parteExtraordinaria) : '—'),
  },
  {
    chave: 'valorTotal', titulo: 'Valor total',
    renderizar: (c) => (c.valorTotal != null ? formatarMoeda(c.valorTotal) : '—'),
  },
  {
    chave: 'dataPagamento', titulo: 'Pago em',
    renderizar: (c) => (c.dataPagamento ? formatarData(c.dataPagamento) : '—'),
  },
]

const COLUNAS_RESERVAS = [
  { chave: 'areaComumNome', titulo: 'Área' },
  { chave: 'data', titulo: 'Data', renderizar: (r) => formatarData(r.data) },
  {
    chave: 'horaInicio', titulo: 'Horário',
    renderizar: (r) => `${formatarHora(r.horaInicio)}–${formatarHora(r.horaFim)}`,
  },
  { chave: 'situacao', titulo: 'Situação', ordenavel: false, renderizar: (r) => seloReserva(r.situacao) },
]

const COLUNAS_OCORRENCIAS = [
  { chave: 'dataAbertura', titulo: 'Aberta em', renderizar: (o) => formatarDataHora(o.dataAbertura) },
  { chave: 'assunto', titulo: 'Assunto' },
  { chave: 'situacao', titulo: 'Situação', ordenavel: false, renderizar: (o) => seloOcorrencia(o.situacao) },
]

function MinhaArea({ condominioId, unidadeAtiva, carregandoUnidades }) {
  const [competencia, setCompetencia] = useState(competenciaPadrao())
  const [painel, setPainel] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const carregar = useCallback(() => {
    if (!unidadeAtiva) {
      setCarregando(false)
      return
    }
    setCarregando(true)
    setErro('')
    buscarPainelUnidade(condominioId, unidadeAtiva.unidadeId, `${competencia}-01`)
      .then(setPainel)
      .catch((e) => { setErro(e.message); setPainel(null) })
      .finally(() => setCarregando(false))
  }, [condominioId, unidadeAtiva, competencia])

  useEffect(() => {
    carregar()
  }, [carregar])

  if (!unidadeAtiva) {
    return (
      <section>
        <h2>Minha área</h2>
        <p className="aviso-vazio">
          {carregandoUnidades
            ? 'Carregando...'
            : 'Não foi possível identificar nenhuma unidade sua. Se você acabou de ser cadastrado, peça ao síndico para registrar a sua entrada.'}
        </p>
      </section>
    )
  }

  return (
    <section>
      <h2>
        Minha área — {unidadeAtiva.blocoNome}, {unidadeAtiva.unidadeNumero}
      </h2>
      <p className="descricao">
        Tudo o que você precisa acompanhar nesta unidade, em um só lugar:{' '}
        {unidadeAtiva.papeis.map(rotuloPapel).join(' e ').toLowerCase()}.
      </p>

      {carregando && <p className="descricao-curta">Carregando...</p>}
      {erro && <p className="alerta-erro">{erro}</p>}

      {painel && (
        <>
          <h3 className="subtitulo-secao">Avisos</h3>
          {painel.avisos.length === 0 ? (
            <p className="descricao-curta">Nenhum aviso no momento.</p>
          ) : (
            <ul className="lista-leituras-aviso">
              {painel.avisos.map((a) => (
                <li key={a.id}>
                  <strong>{a.titulo}</strong> — {rotuloAlcance(a.alcance)}{' '}
                  <span className={`selo ${a.lido ? 'selo-ativo' : 'selo-inativo'}`}>
                    {a.lido ? 'Lido' : 'Não lido'}
                  </span>
                  <p className="descricao-curta">{a.texto}</p>
                </li>
              ))}
            </ul>
          )}

          <h3 className="subtitulo-secao">Situação financeira da unidade</h3>
          <div className="cards-resumo">
            <div className="card-resumo">
              <span className="card-resumo-titulo">Situação</span>
              <strong className="card-resumo-valor">
                {painel.situacaoFinanceira.emDia ? 'Em dia' : 'Em atraso'}
              </strong>
            </div>
            <div className="card-resumo">
              <span className="card-resumo-titulo">Cobranças em atraso</span>
              <strong className="card-resumo-valor">{painel.situacaoFinanceira.cobrancasEmAtraso}</strong>
            </div>
            <div className="card-resumo">
              <span className="card-resumo-titulo">Valor em atraso</span>
              <strong className="card-resumo-valor">
                {formatarMoeda(painel.situacaoFinanceira.valorEmAtraso)}
              </strong>
            </div>
          </div>
          <TabelaDados
            colunas={COLUNAS_COBRANCAS}
            dados={painel.cobrancas}
            camposBusca={[]}
            mensagemVazia="Nenhuma cobrança para esta unidade."
            obterChave={(c) => c.cobrancaId}
          />

          <h3 className="subtitulo-secao">Reservas</h3>
          {painel.reservas ? (
            <TabelaDados
              colunas={COLUNAS_RESERVAS}
              dados={painel.reservas}
              camposBusca={['areaComumNome']}
              buscaPlaceholder="Buscar por área..."
              mensagemVazia="Nenhuma reserva para esta unidade."
            />
          ) : (
            <p className="aviso-vazio">
              Você não mora nesta unidade, então não pode reservar áreas comuns por ela.
            </p>
          )}

          <h3 className="subtitulo-secao">Ocorrências</h3>
          <TabelaDados
            colunas={COLUNAS_OCORRENCIAS}
            dados={painel.ocorrencias}
            camposBusca={['assunto']}
            buscaPlaceholder="Buscar por assunto..."
            mensagemVazia="Nenhuma ocorrência para esta unidade."
          />

          <h3 className="subtitulo-secao">O que você pode fazer nesta unidade</h3>
          <ul>
            <li>Autorizar visita: {painel.permissoes.autorizarVisita ? 'sim' : 'não'}</li>
            <li>Reservar área comum: {painel.permissoes.reservarAreaComum ? 'sim' : 'não'}</li>
            <li>Abrir ocorrência: {painel.permissoes.abrirOcorrencia ? 'sim' : 'não'}</li>
            <li>Ver prestação de contas: {painel.permissoes.verPrestacaoContas ? 'sim' : 'não'}</li>
          </ul>

          <h3 className="subtitulo-secao">
            Prestação de contas {painel.prestacaoContas && `— ${formatarCompetencia(painel.prestacaoContas.periodo.competencia)}`}
          </h3>
          <label className="campo">
            <span>Mês</span>
            <input
              type="month"
              value={competencia}
              onChange={(e) => setCompetencia(e.target.value)}
            />
          </label>
          {painel.prestacaoContas ? (
            <>
              <div className="cards-resumo">
                <div className="card-resumo">
                  <span className="card-resumo-titulo">Receita do período</span>
                  <strong className="card-resumo-valor">
                    {formatarMoeda(painel.prestacaoContas.periodo.receita)}
                  </strong>
                </div>
                <div className="card-resumo">
                  <span className="card-resumo-titulo">Despesa do período</span>
                  <strong className="card-resumo-valor">
                    {formatarMoeda(painel.prestacaoContas.periodo.totalDespesa)}
                  </strong>
                </div>
                <div className="card-resumo">
                  <span className="card-resumo-titulo">Saldo</span>
                  <strong className="card-resumo-valor">
                    {formatarMoeda(painel.prestacaoContas.periodo.saldo)}
                  </strong>
                </div>
                <div className="card-resumo">
                  <span className="card-resumo-titulo">Inadimplência acumulada</span>
                  <strong className="card-resumo-valor">
                    {formatarMoeda(painel.prestacaoContas.inadimplenciaAcumulada)}
                  </strong>
                </div>
              </div>
              <TabelaDados
                colunas={COLUNAS_DESPESAS}
                dados={painel.prestacaoContas.periodo.despesasPorCategoria ?? []}
                camposBusca={['categoria']}
                buscaPlaceholder="Buscar por categoria..."
                mensagemVazia="Nenhuma despesa no período."
                obterChave={(item) => item.categoria}
              />
            </>
          ) : (
            <p className="aviso-vazio">A prestação de contas deste mês ainda não foi publicada.</p>
          )}
        </>
      )}
    </section>
  )
}

export default MinhaArea
