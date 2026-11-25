import { useState } from 'react'
import { darBaixaCobranca } from '../services/api.js'
import Modal from './Modal.jsx'

function hoje() {
  const d = new Date()
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

function DarBaixaModal({ cobranca, condominioId, aoSalvar, aoFechar }) {
  const [valorPago, setValorPago] = useState(String(cobranca.valorTotal))
  const [dataPagamento, setDataPagamento] = useState(hoje())
  const [formaPagamento, setFormaPagamento] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function salvar(evento) {
    evento.preventDefault()
    setErro('')

    if (!formaPagamento) {
      setErro('Selecione a forma de pagamento.')
      return
    }

    setEnviando(true)
    try {
      const cobrancaAtualizada = await darBaixaCobranca(condominioId, cobranca.id, {
        valorPago: Number(valorPago),
        dataPagamento,
        formaPagamento,
      })
      aoSalvar(cobrancaAtualizada)
      aoFechar()
    } catch (erroRequisicao) {
      setErro(erroRequisicao.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal titulo={`Dar baixa — ${cobranca.blocoNome} ${cobranca.unidadeNumero} (andar ${cobranca.unidadeAndar})`} aoFechar={aoFechar}>
      <form onSubmit={salvar} className="formulario">
        <p className="descricao-curta">
          Valor devido: <strong>{formatarMoeda(cobranca.valorTotal)}</strong>
        </p>

        <div className="linha-dois-campos">
          <label className="campo">
            <span>Valor pago</span>
            <input
              type="number"
              step="0.01"
              min="0.01"
              value={valorPago}
              onChange={(e) => setValorPago(e.target.value)}
              required
            />
          </label>

          <label className="campo">
            <span>Data do pagamento</span>
            <input
              type="date"
              value={dataPagamento}
              onChange={(e) => setDataPagamento(e.target.value)}
              required
            />
          </label>
        </div>

        <label className="campo">
          <span>Forma de pagamento</span>
          <select value={formaPagamento} onChange={(e) => setFormaPagamento(e.target.value)}>
            <option value="">Selecione</option>
            <option value="DINHEIRO">Dinheiro</option>
            <option value="PIX">Pix</option>
            <option value="TRANSFERENCIA">Transferência</option>
            <option value="OUTRO">Outro</option>
          </select>
        </label>

        {erro && <p className="alerta-erro">{erro}</p>}

        <div className="linha-dois-campos">
          <button type="submit" className="botao-primario" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Confirmar baixa'}
          </button>
          <button type="button" className="botao-secundario" onClick={aoFechar}>
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default DarBaixaModal