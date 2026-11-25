import { useEffect, useState } from 'react'
import { buscarPreviaRateio, fecharRateio, listarFechamentos } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function formatarData(data) {
  if (!data) return ''
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}

const COLUNAS = [
  {
    chave: 'unidadeNumero',
    titulo: 'Unidade',
    renderizar: (item) => `${item.blocoNome} ${item.unidadeNumero} (andar ${item.unidadeAndar})`,
  },
  {
    chave: 'valorOrdinario',
    titulo: 'Ordinário',
    renderizar: (item) => formatarMoeda(item.valorOrdinario),
  },
  {
    chave: 'valorExtraordinario',
    titulo: 'Extraordinário',
    renderizar: (item) => formatarMoeda(item.valorExtraordinario),
  },
  { chave: 'valorTotal', titulo: 'Total', renderizar: (item) => formatarMoeda(item.valorTotal) },
  { chave: 'responsavelOrdinarioNome', titulo: 'Responsável (ordinário)' },
  { chave: 'responsavelExtraordinarioNome', titulo: 'Responsável (extraordinário)' },
]

function PaginaRateio({ condominioId }) {
  const [mesSelecionado, setMesSelecionado] = useState('')
  const [previa, setPrevia] = useState(null)
  const [carregandoPrevia, setCarregandoPrevia] = useState(false)
  const [erroPrevia, setErroPrevia] = useState('')

  const [fechamentos, setFechamentos] = useState([])
  const [erroFechamentos, setErroFechamentos] = useState('')

  const [fechando, setFechando] = useState(false)
  const [erroFechar, setErroFechar] = useState('')
  const [confirmacaoFechar, setConfirmacaoFechar] = useState('')

  const competenciaFormatada = mesSelecionado ? `${mesSelecionado}-01` : ''
  const jaFechado = fechamentos.find((f) => f.competencia === competenciaFormatada)

  useEffect(() => {
    listarFechamentos(condominioId)
      .then(setFechamentos)
      .catch((erro) => setErroFechamentos(erro.message))
  }, [condominioId])

  function mudarMes(evento) {
    setMesSelecionado(evento.target.value)
    setPrevia(null)
    setErroPrevia('')
    setErroFechar('')
    setConfirmacaoFechar('')
  }

  async function calcularPrevia() {
    if (!competenciaFormatada) return

    setCarregandoPrevia(true)
    setErroPrevia('')
    setConfirmacaoFechar('')
    try {
      const resultado = await buscarPreviaRateio(condominioId, competenciaFormatada)
      setPrevia(resultado)
    } catch (erro) {
      setErroPrevia(erro.message)
      setPrevia(null)
    } finally {
      setCarregandoPrevia(false)
    }
  }

  async function fecharMes() {
    const confirmar = window.confirm(
      `Fechar o rateio de ${mesSelecionado}? Essa ação não pode ser desfeita.`
    )
    if (!confirmar) return

    setFechando(true)
    setErroFechar('')
    try {
      const novoFechamento = await fecharRateio(condominioId, competenciaFormatada)
      setFechamentos((anterior) => [...anterior, novoFechamento])
      setConfirmacaoFechar(
        `Rateio de ${formatarData(novoFechamento.competencia)} fechado com sucesso.`
      )
    } catch (erro) {
      setErroFechar(erro.message)
    } finally {
      setFechando(false)
    }
  }

  return (
    <section>
      <h2>Rateio do mês</h2>
      <p className="descricao">
        Calcule a prévia do rateio antes de fechar — depois de fechado, o mês não pode mais ser
        alterado.
      </p>

      {erroFechamentos && <p className="alerta-erro">{erroFechamentos}</p>}

      <div className="formulario-inline">
        <label className="campo">
          <span>Competência</span>
          <input type="month" value={mesSelecionado} onChange={mudarMes} />
        </label>

        <button
          type="button"
          className="botao-primario"
          onClick={calcularPrevia}
          disabled={!mesSelecionado || carregandoPrevia || Boolean(jaFechado)}
        >
          {carregandoPrevia ? 'Calculando...' : 'Calcular prévia'}
        </button>
      </div>

      {erroPrevia && <p className="alerta-erro">{erroPrevia}</p>}

      {jaFechado && (
        <p className="aviso-vazio">
          Este mês já foi fechado em {formatarData(jaFechado.dataFechamento)} por{' '}
          {jaFechado.fechadoPorNome}. Rateios fechados não podem ser refeitos.
        </p>
      )}

      {previa && (
        <>
          <p className="total-despesas">
            Total ordinário: <strong>{formatarMoeda(previa.valorTotalOrdinario)}</strong> · Total
            extraordinário: <strong>{formatarMoeda(previa.valorTotalExtraordinario)}</strong>
          </p>

          <TabelaDados
            colunas={COLUNAS}
            dados={previa.unidades}
            camposBusca={['unidadeNumero', 'responsavelOrdinarioNome', 'responsavelExtraordinarioNome']}
            buscaPlaceholder="Buscar por unidade ou responsável..."
            mensagemVazia="Nenhuma unidade encontrada."
            obterChave={(item) => item.unidadeId}
          />

          {erroFechar && <p className="alerta-erro">{erroFechar}</p>}
          {confirmacaoFechar && <p className="confirmacao">{confirmacaoFechar}</p>}

          {!jaFechado && (
            <button
              type="button"
              className="botao-primario"
              onClick={fecharMes}
              disabled={fechando}
              style={{ marginTop: '16px' }}
            >
              {fechando ? 'Fechando...' : 'Fechar rateio deste mês'}
            </button>
          )}
        </>
      )}
    </section>
  )
}

export default PaginaRateio