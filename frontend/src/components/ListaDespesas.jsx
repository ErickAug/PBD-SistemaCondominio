import { useMemo, useState } from 'react'
import TabelaDados from './TabelaDados.jsx'
import { excluirDespesa } from '../services/api.js'

const ROTULOS_TIPO = {
  ORDINARIA: 'Ordinária',
  EXTRAORDINARIA: 'Extraordinária',
}

function formatarData(data) {
  if (!data) return ''
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}

function formatarMoeda(valor) {
  return Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

const COLUNAS_BASE = [
  { chave: 'data', titulo: 'Data', renderizar: (item) => formatarData(item.data) },
  { chave: 'categoria', titulo: 'Categoria' },
  { chave: 'fornecedor', titulo: 'Fornecedor' },
  { chave: 'valor', titulo: 'Valor', renderizar: (item) => formatarMoeda(item.valor) },
  {
    chave: 'tipo',
    titulo: 'Tipo',
    renderizar: (item) => ROTULOS_TIPO[item.tipo] ?? item.tipo,
  },
  {
    chave: 'comprovante',
    titulo: 'Comprovante',
    ordenavel: false,
    renderizar: (item) => item.comprovante || '—',
  },
]

function ListaDespesas({ despesas, carregando, erro, condominioId, aoEditar, aoExcluirSucesso }) {
  const [dataInicio, setDataInicio] = useState('')
  const [dataFim, setDataFim] = useState('')
  const [categoriaFiltro, setCategoriaFiltro] = useState('TODAS')
  const [tipoFiltro, setTipoFiltro] = useState('TODOS')
  const [erroExclusao, setErroExclusao] = useState('')
  const [excluindoId, setExcluindoId] = useState(null)

  async function excluir(despesa) {
    const confirmar = window.confirm(
      `Excluir a despesa de ${despesa.categoria} (${formatarMoeda(despesa.valor)})?`
    )
    if (!confirmar) return

    setErroExclusao('')
    setExcluindoId(despesa.id)
    try {
      await excluirDespesa(condominioId, despesa.id)
      aoExcluirSucesso(despesa.id)
    } catch (erroRequisicao) {
      setErroExclusao(erroRequisicao.message)
    } finally {
      setExcluindoId(null)
    }
  }

  const categoriasDisponiveis = useMemo(() => {
    const unicas = new Set(despesas.map((despesa) => despesa.categoria))
    return Array.from(unicas).sort()
  }, [despesas])

  const despesasFiltradas = useMemo(() => {
    return despesas.filter((despesa) => {
      if (dataInicio && despesa.data < dataInicio) return false
      if (dataFim && despesa.data > dataFim) return false
      if (categoriaFiltro !== 'TODAS' && despesa.categoria !== categoriaFiltro) return false
      if (tipoFiltro !== 'TODOS' && despesa.tipo !== tipoFiltro) return false
      return true
    })
  }, [despesas, dataInicio, dataFim, categoriaFiltro, tipoFiltro])

  const totalFiltrado = useMemo(
    () => despesasFiltradas.reduce((soma, despesa) => soma + Number(despesa.valor), 0),
    [despesasFiltradas]
  )

  const colunas = useMemo(
    () => [
      ...COLUNAS_BASE,
      {
        chave: 'acoes',
        titulo: 'Ações',
        ordenavel: false,
        renderizar: (item) => (
          <div className="tabela-dados-acoes">
            <button type="button" className="botao-secundario" onClick={() => aoEditar(item)}>
              Editar
            </button>
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
    <>
      {erro && <p className="alerta-erro">{erro}</p>}

      <div className="formulario-inline">
        <label className="campo">
          <span>De</span>
          <input type="date" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)} />
        </label>

        <label className="campo">
          <span>Até</span>
          <input type="date" value={dataFim} onChange={(e) => setDataFim(e.target.value)} />
        </label>

        <label className="campo">
          <span>Categoria</span>
          <select value={categoriaFiltro} onChange={(e) => setCategoriaFiltro(e.target.value)}>
            <option value="TODAS">Todas</option>
            {categoriasDisponiveis.map((categoria) => (
              <option key={categoria} value={categoria}>
                {categoria}
              </option>
            ))}
          </select>
        </label>

        <label className="campo">
          <span>Tipo</span>
          <select value={tipoFiltro} onChange={(e) => setTipoFiltro(e.target.value)}>
            <option value="TODOS">Todos</option>
            <option value="ORDINARIA">Ordinária</option>
            <option value="EXTRAORDINARIA">Extraordinária</option>
          </select>
        </label>
      </div>

      <p className="total-despesas">
        Total do período filtrado: <strong>{formatarMoeda(totalFiltrado)}</strong>
      </p>

      {carregando ? (
        <p className="descricao">Carregando despesas...</p>
      ) : (
        <TabelaDados
          colunas={colunas}
          dados={despesasFiltradas}
          camposBusca={['categoria', 'fornecedor']}
          buscaPlaceholder="Buscar por categoria ou fornecedor..."
          mensagemVazia="Nenhuma despesa encontrada para os filtros selecionados."
          mensagemErro={erroExclusao}
        />
      )}
    </>
  )
}

export default ListaDespesas