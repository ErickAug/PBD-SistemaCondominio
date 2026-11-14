import { useEffect, useState } from 'react'
import { listarDespesas } from '../services/api.js'
import CadastroDespesa from './CadastroDespesa.jsx'
import ListaDespesas from './ListaDespesas.jsx'
import EditarDespesaModal from './EditarDespesaModal.jsx'

function PaginaDespesas({ condominioId }) {
  const [despesas, setDespesas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [despesaEmEdicao, setDespesaEmEdicao] = useState(null)

  useEffect(() => {
    let ativo = true

    async function carregar() {
      setCarregando(true)
      setErro('')
      try {
        const resultado = await listarDespesas(condominioId)
        if (ativo) setDespesas(resultado.despesas)
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
  }, [condominioId])

  function aoCadastrarDespesa(novaDespesa) {
    setDespesas((anterior) => [...anterior, novaDespesa])
  }

  function aoAtualizarDespesa(despesaAtualizada) {
    setDespesas((anterior) =>
      anterior.map((despesa) => (despesa.id === despesaAtualizada.id ? despesaAtualizada : despesa))
    )
  }

  function aoExcluirDespesa(despesaId) {
    setDespesas((anterior) => anterior.filter((despesa) => despesa.id !== despesaId))
  }

  return (
    <>
      <CadastroDespesa condominioId={condominioId} aoCadastrar={aoCadastrarDespesa} />

      <h3 className="lista-titulo">Despesas lançadas</h3>
      <ListaDespesas
        despesas={despesas}
        carregando={carregando}
        erro={erro}
        condominioId={condominioId}
        aoEditar={setDespesaEmEdicao}
        aoExcluirSucesso={aoExcluirDespesa}
      />

      {despesaEmEdicao && (
        <EditarDespesaModal
          despesa={despesaEmEdicao}
          condominioId={condominioId}
          aoSalvar={aoAtualizarDespesa}
          aoFechar={() => setDespesaEmEdicao(null)}
        />
      )}
    </>
  )
}

export default PaginaDespesas