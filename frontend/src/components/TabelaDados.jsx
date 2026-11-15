import { useMemo, useState, useEffect } from 'react'

const OPCOES_ITENS_POR_PAGINA = [10, 25, 50]

function compararValores(a, b) {
  if (a === null || a === undefined) return -1
  if (b === null || b === undefined) return 1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), 'pt-BR', { numeric: true })
}

function TabelaDados({
  colunas,
  dados,
  camposBusca = [],
  buscaPlaceholder = 'Buscar...',
  mensagemVazia = 'Nenhum registro encontrado.',
  mensagemErro = '',
  obterChave = (item) => item.id,
}) {
  const [termoBusca, setTermoBusca] = useState('')
  const [ordenacao, setOrdenacao] = useState({ chave: null, direcao: 'asc' })
  const [paginaAtual, setPaginaAtual] = useState(1)
  const [itensPorPagina, setItensPorPagina] = useState(OPCOES_ITENS_POR_PAGINA[0])

  useEffect(() => {
    setPaginaAtual(1)
  }, [termoBusca, dados, itensPorPagina])

  const dadosFiltrados = useMemo(() => {
    if (!termoBusca.trim()) return dados

    const termo = termoBusca.trim().toLowerCase()
    return dados.filter((item) =>
      camposBusca.some((campo) => String(item[campo] ?? '').toLowerCase().includes(termo))
    )
  }, [dados, termoBusca, camposBusca])

  const dadosOrdenados = useMemo(() => {
    if (!ordenacao.chave) return dadosFiltrados

    const copia = [...dadosFiltrados]
    copia.sort((a, b) => {
      const resultado = compararValores(a[ordenacao.chave], b[ordenacao.chave])
      return ordenacao.direcao === 'asc' ? resultado : -resultado
    })
    return copia
  }, [dadosFiltrados, ordenacao])

  const totalPaginas = Math.max(1, Math.ceil(dadosOrdenados.length / itensPorPagina))
  const paginaSegura = Math.min(paginaAtual, totalPaginas)
  const inicio = (paginaSegura - 1) * itensPorPagina
  const dadosPaginados = dadosOrdenados.slice(inicio, inicio + itensPorPagina)

  function alternarOrdenacao(chave) {
    setOrdenacao((anterior) => {
      if (anterior.chave !== chave) return { chave, direcao: 'asc' }
      if (anterior.direcao === 'asc') return { chave, direcao: 'desc' }
      return { chave: null, direcao: 'asc' }
    })
  }

  function indicadorOrdenacao(chave) {
    if (ordenacao.chave !== chave) return ''
    return ordenacao.direcao === 'asc' ? ' ▲' : ' ▼'
  }

  return (
    <div className="tabela-dados-container">
      <div className="tabela-dados-toolbar">
        <label className="campo campo-busca-tabela">
          <span>Buscar</span>
          <input
            value={termoBusca}
            onChange={(evento) => setTermoBusca(evento.target.value)}
            placeholder={buscaPlaceholder}
          />
        </label>

        <label className="campo campo-itens-pagina">
          <span>Itens por página</span>
          <select
            value={itensPorPagina}
            onChange={(evento) => setItensPorPagina(Number(evento.target.value))}
          >
            {OPCOES_ITENS_POR_PAGINA.map((opcao) => (
              <option key={opcao} value={opcao}>
                {opcao}
              </option>
            ))}
          </select>
        </label>
      </div>

      {mensagemErro && <p className="alerta-erro">{mensagemErro}</p>}

      <div className="tabela-dados-rolagem">
        <table className="tabela-dados">
          <thead>
            <tr>
              {colunas.map((coluna) => (
                <th
                  key={coluna.chave}
                  className={coluna.ordenavel === false ? '' : 'coluna-ordenavel'}
                  onClick={() => coluna.ordenavel !== false && alternarOrdenacao(coluna.chave)}
                >
                  {coluna.titulo}
                  {coluna.ordenavel !== false && indicadorOrdenacao(coluna.chave)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {dadosPaginados.length === 0 && (
              <tr>
                <td colSpan={colunas.length} className="linha-tabela-vazia">
                  {mensagemVazia}
                </td>
              </tr>
            )}

            {dadosPaginados.map((item) => (
              <tr key={obterChave(item)}>
                {colunas.map((coluna) => (
                  <td key={coluna.chave}>
                    {coluna.renderizar ? coluna.renderizar(item) : item[coluna.chave]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="tabela-dados-paginacao">
        <span className="tabela-dados-contagem">
          {dadosOrdenados.length} registro(s)
        </span>

        <div className="tabela-dados-paginacao-botoes">
          <button
            type="button"
            className="botao-secundario"
            onClick={() => setPaginaAtual((p) => Math.max(1, p - 1))}
            disabled={paginaSegura === 1}
          >
            Anterior
          </button>
          <span className="tabela-dados-pagina-atual">
            Página {paginaSegura} de {totalPaginas}
          </span>
          <button
            type="button"
            className="botao-secundario"
            onClick={() => setPaginaAtual((p) => Math.min(totalPaginas, p + 1))}
            disabled={paginaSegura === totalPaginas}
          >
            Próxima
          </button>
        </div>
      </div>
    </div>
  )
}

export default TabelaDados