import { useState } from 'react'
import { buscarRelatorioPeriodo, baixarRelatorioCsv } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatarCompetencia(competencia) {
  if (!competencia) return ''
  const [ano, mes] = competencia.split('-')
  return `${mes}/${ano}`
}

function formatarDiferenca(valorAtual, valorAnterior) {
  const diferenca = Number(valorAtual ?? 0) - Number(valorAnterior ?? 0)
  const sinal = diferenca > 0 ? '+' : ''
  return `${sinal}${formatarMoeda(diferenca)}`
}

const COLUNAS_DESPESAS = [
  { chave: 'categoria', titulo: 'Categoria' },
  {
    chave: 'valor',
    titulo: 'Valor',
    renderizar: (item) => formatarMoeda(item.valor),
  },
]

function PaginaPrestacaoContas({ condominioId }) {
  const [mesSelecionado, setMesSelecionado] = useState('')
  const [relatorio, setRelatorio] = useState(null)
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  const [exportando, setExportando] = useState(false)
  const [erroExportar, setErroExportar] = useState('')

  const competenciaFormatada = mesSelecionado ? `${mesSelecionado}-01` : ''

  function mudarMes(evento) {
    setMesSelecionado(evento.target.value)
    setRelatorio(null)
    setErro('')
    setErroExportar('')
  }

  async function gerarRelatorio() {
    if (!competenciaFormatada) return

    setCarregando(true)
    setErro('')
    setErroExportar('')
    try {
      const resultado = await buscarRelatorioPeriodo(condominioId, competenciaFormatada)
      setRelatorio(resultado)
    } catch (erroRequisicao) {
      setErro(erroRequisicao.message)
      setRelatorio(null)
    } finally {
      setCarregando(false)
    }
  }

  async function exportarCsv() {
    if (!competenciaFormatada) return

    setExportando(true)
    setErroExportar('')
    try {
      const blob = await baixarRelatorioCsv(condominioId, competenciaFormatada)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `prestacao-contas-${competenciaFormatada}.csv`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (erroRequisicao) {
      setErroExportar(erroRequisicao.message)
    } finally {
      setExportando(false)
    }
  }

  return (
    <section>
      <h2>Prestação de contas do período</h2>
      <p className="descricao">
        Receita, despesa por categoria, saldo e inadimplência acumulada do mês, já comparado com o
        mês anterior — pronto para levar à assembleia. Também pode ser exportado em CSV.
      </p>

      <div className="formulario-inline">
        <label className="campo">
          <span>Competência</span>
          <input type="month" value={mesSelecionado} onChange={mudarMes} />
        </label>

        <button
          type="button"
          className="botao-primario"
          onClick={gerarRelatorio}
          disabled={!mesSelecionado || carregando}
        >
          {carregando ? 'Gerando...' : 'Gerar relatório'}
        </button>
      </div>

      {erro && <p className="alerta-erro">{erro}</p>}

      {relatorio && (
        <>
          <div className="cards-resumo">
            <div className="card-resumo">
              <span className="card-resumo-titulo">Receita do período</span>
              <strong className="card-resumo-valor">{formatarMoeda(relatorio.periodo.receita)}</strong>
              <span className="card-resumo-diferenca">
                {formatarDiferenca(relatorio.periodo.receita, relatorio.mesAnterior.receita)} vs.{' '}
                {formatarCompetencia(relatorio.mesAnterior.competencia)}
              </span>
            </div>

            <div className="card-resumo">
              <span className="card-resumo-titulo">Despesa do período</span>
              <strong className="card-resumo-valor">
                {formatarMoeda(relatorio.periodo.totalDespesa)}
              </strong>
              <span className="card-resumo-diferenca">
                {formatarDiferenca(relatorio.periodo.totalDespesa, relatorio.mesAnterior.totalDespesa)}{' '}
                vs. {formatarCompetencia(relatorio.mesAnterior.competencia)}
              </span>
            </div>

            <div className="card-resumo">
              <span className="card-resumo-titulo">Saldo</span>
              <strong className="card-resumo-valor">{formatarMoeda(relatorio.periodo.saldo)}</strong>
              <span className="card-resumo-diferenca">
                {formatarDiferenca(relatorio.periodo.saldo, relatorio.mesAnterior.saldo)} vs.{' '}
                {formatarCompetencia(relatorio.mesAnterior.competencia)}
              </span>
            </div>

            <div className="card-resumo">
              <span className="card-resumo-titulo">Inadimplência acumulada</span>
              <strong className="card-resumo-valor">
                {formatarMoeda(relatorio.inadimplenciaAcumulada)}
              </strong>
            </div>
          </div>

          <h3 className="subtitulo-secao">Despesas por categoria</h3>
          <TabelaDados
            colunas={COLUNAS_DESPESAS}
            dados={relatorio.periodo.despesasPorCategoria ?? []}
            camposBusca={['categoria']}
            buscaPlaceholder="Buscar por categoria..."
            mensagemVazia="Nenhuma despesa no período."
            obterChave={(item) => item.categoria}
          />

          {erroExportar && <p className="alerta-erro">{erroExportar}</p>}

          <button
            type="button"
            className="botao-primario"
            onClick={exportarCsv}
            disabled={exportando}
            style={{ marginTop: '16px' }}
          >
            {exportando ? 'Exportando...' : 'Exportar CSV'}
          </button>
        </>
      )}
    </section>
  )
}

export default PaginaPrestacaoContas