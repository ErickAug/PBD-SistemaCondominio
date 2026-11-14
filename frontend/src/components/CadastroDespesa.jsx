import { useState } from 'react'
import { cadastrarDespesa } from '../services/api.js'
import CamposDespesa from './CamposDespesa.jsx'

const ESTADO_INICIAL = {
  data: '',
  categoria: '',
  fornecedor: '',
  valor: '',
  tipo: 'ORDINARIA',
  comprovante: '',
  numeroParcelas: '',
}

function CadastroDespesa({ condominioId, aoCadastrar }) {
  const [form, setForm] = useState(ESTADO_INICIAL)
  const [erro, setErro] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [enviando, setEnviando] = useState(false)

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErro('')
    setConfirmacao('')
  }

  async function lancarDespesa(evento) {
    evento.preventDefault()
    setEnviando(true)
    setErro('')
    setConfirmacao('')

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

      const novaDespesa = await cadastrarDespesa(condominioId, dados)
      aoCadastrar(novaDespesa)
      setForm(ESTADO_INICIAL)
      setConfirmacao('Despesa lançada com sucesso.')
    } catch (erroRequisicao) {
      setErro(erroRequisicao.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section>
      <h2>Lançar despesa</h2>
      <p className="descricao">
        Registre o que o condomínio gastou para que o rateio do mês saia dos números reais.
      </p>

      <form onSubmit={lancarDespesa} className="formulario">
        <CamposDespesa form={form} aoMudarCampo={atualizarCampo} />

        {erro && <p className="alerta-erro">{erro}</p>}
        {confirmacao && <p className="confirmacao">{confirmacao}</p>}

        <button type="submit" className="botao-primario" disabled={enviando}>
          {enviando ? 'Salvando...' : 'Lançar despesa'}
        </button>
      </form>
    </section>
  )
}

export default CadastroDespesa