import { useEffect, useState } from 'react'
import { buscarRelatorioPeriodo } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

function formatarMoeda(valor) {
  return Number(valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatarCompetencia(competencia) {
  if (!competencia) return ''
  const [ano, mes] = competencia.split('-')
  return `${mes}/${ano}`
}

function competenciaAtual() {
  const hoje = new Date()
  const ano = hoje.getFullYear()
  const mes = String(hoje.getMonth() + 1).padStart(2, '0')
  return `${ano}-${mes}-01`
}

const COLUNAS_DESPESAS = [
  { chave: 'categoria', titulo: 'Categoria' },
  {
    chave: 'valor',
    titulo: 'Valor',
    renderizar: (item) => formatarMoeda(item.valor),
  },
]

function PrestacaoContasUnidade({ condominioId }) {
  const [relatorio, setRelatorio] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    setCarregando(true)
    setErro('')
    buscarRelatorioPeriodo(condominioId, competenciaAtual())
      .then(setRelatorio)
      .catch((erroRequisicao) => setErro(erroRequisicao.message))
      .finally(() => setCarregando(false))
  }, [condominioId])

  if (carregando) {
    return (
      <section>
        <h2>Prestação de contas</h2>
        <p className="descricao">Carregando...</p>
      </section>
    )
  }

  if (erro) {
    return (
      <section>
        <h2>Prestação de contas</h2>
        <p className="alerta-erro">{erro}</p>
      </section>
    )
  }

  if (!relatorio) {
    return (
      <section>
        <h2>Prestação de contas</h2>
        <p className="aviso-vazio">A prestação de contas deste mês ainda não foi publicada.</p>
      </section>
    )
  }

  return (
    <section>
      <h2>Prestação de contas — {formatarCompetencia(relatorio.periodo.competencia)}</h2>
      <p className="descricao">O que entrou e o que saiu do condomínio neste período.</p>

      <div className="cards-resumo">
        <div className="card-resumo">
          <span className="card-resumo-titulo">Receita do período</span>
          <strong className="card-resumo-valor">{formatarMoeda(relatorio.periodo.receita)}</strong>
        </div>

        <div className="card-resumo">
          <span className="card-resumo-titulo">Despesa do período</span>
          <strong className="card-resumo-valor">
            {formatarMoeda(relatorio.periodo.totalDespesa)}
          </strong>
        </div>

        <div className="card-resumo">
          <span className="card-resumo-titulo">Saldo</span>
          <strong className="card-resumo-valor">{formatarMoeda(relatorio.periodo.saldo)}</strong>
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
    </section>
  )
}

export default PrestacaoContasUnidade