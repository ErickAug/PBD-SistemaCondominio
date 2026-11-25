import { useEffect, useMemo, useState } from 'react'
import { listarCobrancas, listarInadimplencia, excluirCobranca } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import DarBaixaModal from './DarBaixaModal.jsx'

const ABAS = [
  { valor: 'todas', rotulo: 'Todas as cobranças' },
  { valor: 'inadimplencia', rotulo: 'Inadimplência' },
]

function formatarData(data) {
  if (!data) return ''
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

const ROTULOS_FORMA_PAGAMENTO = {
  DINHEIRO: 'Dinheiro',
  PIX: 'Pix',
  TRANSFERENCIA: 'Transferência',
  OUTRO: 'Outro',
}

function PaginaCobrancas({ condominioId }) {
  const [aba, setAba] = useState('todas')
  const [competenciaFiltro, setCompetenciaFiltro] = useState('')
  const [situacaoFiltro, setSituacaoFiltro] = useState('')

  const [cobrancas, setCobrancas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [erroAcao, setErroAcao] = useState('')
  const [excluindoId, setExcluindoId] = useState(null)
  const [cobrancaParaBaixa, setCobrancaParaBaixa] = useState(null)

  useEffect(() => {
    let ativo = true

    async function carregar() {
      setCarregando(true)
      setErro('')
      try {
        const competenciaFormatada = competenciaFiltro ? `${competenciaFiltro}-01` : undefined
        const resultado =
          aba === 'inadimplencia'
            ? await listarInadimplencia(condominioId)
            : await listarCobrancas(condominioId, competenciaFormatada, situacaoFiltro || undefined)
        if (ativo) setCobrancas(resultado)
      } catch (erroRequisicao) {
        if (ativo) setErro(erroRequisicao.message)
      } finally {
        if (ativo) setCarregando(false)
      }
    }

    carregar()
    return () => {
      ativo = false
    }
  }, [condominioId, aba, competenciaFiltro, situacaoFiltro])

  function aoBaixarSucesso(cobrancaAtualizada) {
    setCobrancas((anterior) =>
      anterior.map((c) => (c.id === cobrancaAtualizada.id ? cobrancaAtualizada : c))
    )
  }

  async function excluir(cobranca) {
    const confirmar = window.confirm(
      `Excluir a cobrança da unidade ${cobranca.blocoNome} ${cobranca.unidadeNumero} (andar ${cobranca.unidadeAndar}) (${formatarMoeda(cobranca.valorTotal)})?`
    )
    if (!confirmar) return

    setErroAcao('')
    setExcluindoId(cobranca.id)
    try {
      await excluirCobranca(condominioId, cobranca.id)
      setCobrancas((anterior) => anterior.filter((c) => c.id !== cobranca.id))
    } catch (erroRequisicao) {
      setErroAcao(erroRequisicao.message)
    } finally {
      setExcluindoId(null)
    }
  }

  const colunas = useMemo(
    () => [
      {
        chave: 'unidadeNumero',
        titulo: 'Unidade',
        renderizar: (item) => `${item.blocoNome} ${item.unidadeNumero} (andar ${item.unidadeAndar})`,
      },
      { chave: 'competencia', titulo: 'Competência', renderizar: (item) => formatarData(item.competencia) },
      { chave: 'vencimento', titulo: 'Vencimento', renderizar: (item) => formatarData(item.vencimento) },
      {
        chave: 'situacao',
        titulo: 'Situação',
        renderizar: (item) => {
          if (item.situacao === 'PAGA') return <span className="selo selo-ativo">Paga</span>
          if (item.emAtraso)
            return <span className="selo selo-vencido">Em atraso ({item.diasAtraso}d)</span>
          return <span className="selo selo-inativo">Aberta</span>
        },
      },
      { chave: 'valorTotal', titulo: 'Valor', renderizar: (item) => formatarMoeda(item.valorTotal) },
      { chave: 'responsavelOrdinarioNome', titulo: 'Responsável' },
      {
        chave: 'formaPagamento',
        titulo: 'Pagamento',
        ordenavel: false,
        renderizar: (item) =>
          item.situacao === 'PAGA'
            ? `${ROTULOS_FORMA_PAGAMENTO[item.formaPagamento] ?? item.formaPagamento} em ${formatarData(item.dataPagamento)}`
            : '—',
      },
      {
        chave: 'acoes',
        titulo: 'Ações',
        ordenavel: false,
        renderizar: (item) => (
          <div className="tabela-dados-acoes">
            {item.situacao === 'ABERTA' && (
              <button
                type="button"
                className="botao-secundario"
                onClick={() => setCobrancaParaBaixa(item)}
              >
                Dar baixa
              </button>
            )}
            <button
              type="button"
              className="botao-remover"
              onClick={() => excluir(item)}
              disabled={excluindoId === item.id}
            >
              {excluindoId === item.id ? 'Excluindo...' : 'Excluir'}
            </button>
          </div>
        ),
      },
    ],
    [excluindoId]
  )

  return (
    <section>
      <h2>Cobranças</h2>
      <p className="descricao">
        Acompanhe as cobranças geradas pelo rateio, dê baixa manual nos pagamentos e veja quem
        está inadimplente.
      </p>

      <div className="abas">
        {ABAS.map((a) => (
          <button
            key={a.valor}
            type="button"
            className={aba === a.valor ? 'aba ativa' : 'aba'}
            onClick={() => setAba(a.valor)}
          >
            {a.rotulo}
          </button>
        ))}
      </div>

      {aba === 'todas' && (
        <div className="formulario-inline">
          <label className="campo">
            <span>Competência</span>
            <input
              type="month"
              value={competenciaFiltro}
              onChange={(e) => setCompetenciaFiltro(e.target.value)}
            />
          </label>

          <label className="campo">
            <span>Situação</span>
            <select value={situacaoFiltro} onChange={(e) => setSituacaoFiltro(e.target.value)}>
              <option value="">Todas</option>
              <option value="ABERTA">Aberta</option>
              <option value="PAGA">Paga</option>
            </select>
          </label>
        </div>
      )}

      {erro && <p className="alerta-erro">{erro}</p>}

      {carregando ? (
        <p className="descricao">Carregando cobranças...</p>
      ) : (
        <TabelaDados
          colunas={colunas}
          dados={cobrancas}
          camposBusca={['unidadeNumero', 'responsavelOrdinarioNome']}
          buscaPlaceholder="Buscar por unidade ou responsável..."
          mensagemVazia="Nenhuma cobrança encontrada."
          mensagemErro={erroAcao}
        />
      )}

      {cobrancaParaBaixa && (
        <DarBaixaModal
          cobranca={cobrancaParaBaixa}
          condominioId={condominioId}
          aoSalvar={aoBaixarSucesso}
          aoFechar={() => setCobrancaParaBaixa(null)}
        />
      )}
    </section>
  )
}

export default PaginaCobrancas