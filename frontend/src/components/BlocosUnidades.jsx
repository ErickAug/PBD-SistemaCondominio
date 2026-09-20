import { useState, useEffect, useCallback } from 'react'
import {
  cadastrarBloco as apiCadastrarBloco,
  listarBlocos as apiListarBlocos,
  cadastrarUnidade as apiCadastrarUnidade,
  listarUnidades as apiListarUnidades,
  excluirUnidade as apiExcluirUnidade,
  buscarStatusFracaoIdeal,
} from '../services/api.js'

const FORM_UNIDADE_VAZIO = {
  blocoId: '',
  numero: '',
  andar: '',
  area: '',
  fracaoIdeal: '',
}

function BlocosUnidades({ nomeCondominio, condominioId }) {
  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [statusFracao, setStatusFracao] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erroCarregamento, setErroCarregamento] = useState('')

  const [nomeBloco, setNomeBloco] = useState('')
  const [formUnidade, setFormUnidade] = useState(FORM_UNIDADE_VAZIO)
  const [erroUnidade, setErroUnidade] = useState('')
  const [mensagemExclusao, setMensagemExclusao] = useState('')
  const [enviando, setEnviando] = useState(false)

  const carregarTudo = useCallback(async () => {
    setCarregando(true)
    setErroCarregamento('')
    try {
      const [listaBlocos, status] = await Promise.all([
        apiListarBlocos(condominioId),
        buscarStatusFracaoIdeal(condominioId),
      ])
      setBlocos(listaBlocos)
      setStatusFracao(status)

      const listasPorBloco = await Promise.all(
        listaBlocos.map((bloco) => apiListarUnidades(condominioId, bloco.id))
      )
      setUnidades(listasPorBloco.flat())
    } catch (erro) {
      setErroCarregamento(erro.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId])

  useEffect(() => {
    carregarTudo()
  }, [carregarTudo])

  async function cadastrarBloco(evento) {
    evento.preventDefault()
    if (!nomeBloco.trim()) return

    setEnviando(true)
    try {
      await apiCadastrarBloco(condominioId, nomeBloco.trim())
      setNomeBloco('')
      await carregarTudo() 
    } catch (erro) {
      setErroCarregamento(erro.message)
    } finally {
      setEnviando(false)
    }
  }

  function atualizarCampoUnidade(evento) {
    const { name, value } = evento.target
    setFormUnidade((anterior) => ({ ...anterior, [name]: value }))
    setErroUnidade('')
  }

  async function cadastrarUnidade(evento) {
    evento.preventDefault()
    setErroUnidade('')

    const fracao = Number(formUnidade.fracaoIdeal)
    if (fracao < 0 || fracao > 100) {
      setErroUnidade('A fração ideal precisa estar entre 0% e 100%.')
      return
    }

    setEnviando(true)
    try {
      await apiCadastrarUnidade(condominioId, formUnidade.blocoId, {
        numero: formUnidade.numero,
        andar: Number(formUnidade.andar),
        area: Number(formUnidade.area),
        fracaoIdeal: fracao,
      })
      setFormUnidade({ ...FORM_UNIDADE_VAZIO, blocoId: formUnidade.blocoId })
      await carregarTudo() 
    } catch (erro) {
      setErroUnidade(erro.message)
    } finally {
      setEnviando(false)
    }
  }

  async function tentarExcluirUnidade(unidade) {
    setMensagemExclusao('')
    try {
      await apiExcluirUnidade(condominioId, unidade.blocoId, unidade.id)
      await carregarTudo()
    } catch (erro) {
      setMensagemExclusao(erro.message)
    }
  }

  function nomeDoBloco(blocoId) {
    return blocos.find((b) => b.id === blocoId)?.nome ?? '—'
  }

  if (carregando) {
    return (
      <section>
        <h2>Blocos e unidades — {nomeCondominio}</h2>
        <p className="aviso-vazio">Carregando...</p>
      </section>
    )
  }

  return (
    <section>
      <h2>Blocos e unidades — {nomeCondominio}</h2>
      <p className="descricao">
        A soma das frações ideais de todas as unidades precisa fechar
        exatamente 100%, porque é a base do rateio das despesas do
        condomínio.
      </p>

      {erroCarregamento && <p className="erro-login">{erroCarregamento}</p>}

      {statusFracao && (
        <div className={statusFracao.fechaEm100 ? 'painel-fracao ok' : 'painel-fracao alerta'}>
          <strong>{statusFracao.somaAtual.toFixed(2)}%</strong> cadastrado
          {!statusFracao.fechaEm100 && (
            <span>
              {' '}
              — faltam{' '}
              <strong>{statusFracao.diferencaParaFechar.toFixed(2)}%</strong> pra
              fechar 100%
            </span>
          )}
          {statusFracao.fechaEm100 && <span> — fechou certinho ✓</span>}
        </div>
      )}

      <h3 className="lista-titulo">Blocos</h3>
      <form onSubmit={cadastrarBloco} className="formulario formulario-linha">
        <input
          value={nomeBloco}
          onChange={(e) => setNomeBloco(e.target.value)}
          placeholder="Ex: Bloco A"
          disabled={enviando}
        />
        <button type="submit" className="botao-primario" disabled={enviando}>
          {enviando ? 'Salvando...' : 'Adicionar bloco'}
        </button>
      </form>

      <div className="lista-blocos">
        {blocos.length === 0 && (
          <p className="aviso-vazio">Nenhum bloco cadastrado ainda.</p>
        )}
        {blocos.map((bloco) => (
          <span key={bloco.id} className="chip-bloco">
            {bloco.nome}
          </span>
        ))}
      </div>

      <h3 className="lista-titulo">Nova unidade</h3>
      {blocos.length === 0 ? (
        <p className="aviso-vazio">Cadastre um bloco antes de adicionar unidades.</p>
      ) : (
        <form onSubmit={cadastrarUnidade} className="formulario">
          <label className="campo">
            <span>Bloco</span>
            <select
              name="blocoId"
              value={formUnidade.blocoId}
              onChange={atualizarCampoUnidade}
              required
            >
              <option value="" disabled>
                Selecione o bloco
              </option>
              {blocos.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.nome}
                </option>
              ))}
            </select>
          </label>

          <div className="linha-dois-campos">
            <label className="campo">
              <span>Número</span>
              <input
                name="numero"
                value={formUnidade.numero}
                onChange={atualizarCampoUnidade}
                placeholder="Ex: 101"
                required
              />
            </label>
            <label className="campo">
              <span>Andar</span>
              <input
                type="number"
                name="andar"
                value={formUnidade.andar}
                onChange={atualizarCampoUnidade}
                placeholder="Ex: 1"
                required
              />
            </label>
          </div>

          <div className="linha-dois-campos">
            <label className="campo">
              <span>Área (m²)</span>
              <input
                type="number"
                step="0.01"
                name="area"
                value={formUnidade.area}
                onChange={atualizarCampoUnidade}
                required
              />
            </label>
            <label className="campo">
              <span>Fração ideal (%)</span>
              <input
                type="number"
                step="0.01"
                min="0"
                max="100"
                name="fracaoIdeal"
                value={formUnidade.fracaoIdeal}
                onChange={atualizarCampoUnidade}
                required
              />
            </label>
          </div>

          {erroUnidade && <p className="erro-login">{erroUnidade}</p>}

          <button type="submit" className="botao-primario" disabled={enviando}>
            {enviando ? 'Salvando...' : 'Adicionar unidade'}
          </button>
        </form>
      )}

      <h3 className="lista-titulo">
        {unidades.length === 0
          ? 'Nenhuma unidade cadastrada ainda'
          : `${unidades.length} unidade(s) cadastrada(s)`}
      </h3>

      {mensagemExclusao && <p className="erro-login">{mensagemExclusao}</p>}

      <ul className="lista-condominios">
        {unidades.map((unidade) => (
          <li key={unidade.id} className="item-condominio">
            <div>
              <strong>
                {nomeDoBloco(unidade.blocoId)} · unidade {unidade.numero}
              </strong>
              <p>
                {unidade.andar}º andar · {unidade.area} m² · fração{' '}
                {Number(unidade.fracaoIdeal).toFixed(2)}%
              </p>
            </div>
            <div className="item-condominio-acoes">
              <span className={unidade.ocupada ? 'selo selo-inativo' : 'selo selo-ativo'}>
                {unidade.ocupada ? 'Ocupada' : 'Vaga'}
              </span>
              <button
                onClick={() => tentarExcluirUnidade(unidade)}
                className="botao-remover"
                type="button"
              >
                Excluir
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default BlocosUnidades