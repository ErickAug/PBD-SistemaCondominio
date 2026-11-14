import { useState } from 'react'
import { atualizarDespesa } from '../services/api.js'
import Modal from './Modal.jsx'
import CamposDespesa from './CamposDespesa.jsx'

function dadosParaForm(despesa) {
  return {
    data: despesa.data,
    categoria: despesa.categoria,
    fornecedor: despesa.fornecedor,
    valor: despesa.valor,
    tipo: despesa.tipo,
    comprovante: despesa.comprovante,
    numeroParcelas: despesa.tipo === 'EXTRAORDINARIA' ? String(despesa.parcelas?.length ?? '') : '',
  }
}

function EditarDespesaModal({ despesa, condominioId, aoSalvar, aoFechar }) {
  const [form, setForm] = useState(() => dadosParaForm(despesa))
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErro('')
  }

  async function salvar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setErro('')

    try {
      const dados = {
        data: form.data,
        categoria: form.categoria,
        fornecedor: form.fornecedor,
        valor: Number(form.valor),
        tipo: form.tipo,
        comprovante: form.comprovante,
      }

      if (form.tipo === 'EXTRAORDINARIA') {
        dados.numeroParcelas = Number(form.numeroParcelas)
      }

      const despesaAtualizada = await atualizarDespesa(condominioId, despesa.id, dados)
      aoSalvar(despesaAtualizada)
      aoFechar()
    } catch (erroRequisicao) {
      setErro(erroRequisicao.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Modal titulo="Editar despesa" aoFechar={aoFechar}>
      <form onSubmit={salvar} className="formulario">
        <CamposDespesa form={form} aoMudarCampo={atualizarCampo} />

        {erro && <p className="alerta-erro">{erro}</p>}

        <div className="linha-dois-campos">
          <button type="submit" className="botao-primario" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Salvar alterações'}
          </button>
          <button type="button" className="botao-secundario" onClick={aoFechar}>
            Cancelar
          </button>
        </div>
      </form>
    </Modal>
  )
}

export default EditarDespesaModal