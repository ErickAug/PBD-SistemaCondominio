import { useState, useEffect, useCallback } from 'react'
import {
  listarFilaOcorrenciasMorador,
  detalharOcorrenciaMorador,
  responderOcorrenciaMorador,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import Modal from './Modal.jsx'

const TIPOS = {
  RECLAMACAO: 'Reclamação',
  SOLICITACAO: 'Solicitação',
  DEFEITO: 'Defeito',
}

const SITUACOES = {
  ABERTA: { rotulo: 'Aberta', classe: 'selo-inativo' },
  EM_ANALISE: { rotulo: 'Em análise', classe: 'selo-inativo' },
  RESOLVIDA: { rotulo: 'Resolvida', classe: 'selo-ativo' },
  RECUSADA: { rotulo: 'Recusada', classe: 'selo-vencido' },
}

const SITUACOES_RESPOSTA = [
  { valor: 'EM_ANALISE', rotulo: 'Em análise' },
  { valor: 'RESOLVIDA', rotulo: 'Resolvida' },
  { valor: 'RECUSADA', rotulo: 'Recusada' },
]

const SITUACOES_ENCERRADAS = ['RESOLVIDA', 'RECUSADA']

function seloSituacao(situacao) {
  const info = SITUACOES[situacao] ?? { rotulo: situacao, classe: 'selo-inativo' }
  return <span className={`selo ${info.classe}`}>{info.rotulo}</span>
}

function formatarDataHora(dataHora) {
  if (!dataHora) return ''
  return new Date(dataHora).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

function FilaOcorrenciasMorador({ condominioId }) {
  const [situacaoFiltro, setSituacaoFiltro] = useState('')
  const [fila, setFila] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroLista, setErroLista] = useState('')

  const [ocorrenciaSelecionada, setOcorrenciaSelecionada] = useState(null)
  const [detalhe, setDetalhe] = useState(null)
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false)
  const [erroDetalhe, setErroDetalhe] = useState('')

  const [novaSituacao, setNovaSituacao] = useState('')
  const [resposta, setResposta] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erroResposta, setErroResposta] = useState('')

  const carregar = useCallback(() => {
    setCarregando(true)
    setErroLista('')
    listarFilaOcorrenciasMorador(condominioId, situacaoFiltro || null)
      .then(setFila)
      .catch((e) => { setErroLista(e.message); setFila([]) })
      .finally(() => setCarregando(false))
  }, [condominioId, situacaoFiltro])

  useEffect(() => {
    carregar()
  }, [carregar])

  async function abrirDetalhe(ocorrencia) {
    setOcorrenciaSelecionada(ocorrencia)
    setDetalhe(null)
    setErroDetalhe('')
    setNovaSituacao('')
    setResposta('')
    setErroResposta('')
    setCarregandoDetalhe(true)
    try {
      setDetalhe(await detalharOcorrenciaMorador(condominioId, ocorrencia.id))
    } catch (e) {
      setErroDetalhe(e.message)
    } finally {
      setCarregandoDetalhe(false)
    }
  }

  function fecharDetalhe() {
    setOcorrenciaSelecionada(null)
    setDetalhe(null)
    setErroDetalhe('')
  }

  async function enviarResposta(evento) {
    evento.preventDefault()
    setErroResposta('')
    if (!novaSituacao) { setErroResposta('Selecione a nova situação.'); return }
    if (!resposta.trim()) { setErroResposta('Escreva uma resposta para o morador.'); return }

    setSalvando(true)
    try {
      const atualizada = await responderOcorrenciaMorador(condominioId, ocorrenciaSelecionada.id, {
        situacao: novaSituacao,
        resposta: resposta.trim(),
      })
      setDetalhe(atualizada)
      setNovaSituacao('')
      setResposta('')
      carregar()
    } catch (e) {
      setErroResposta(e.message)
    } finally {
      setSalvando(false)
    }
  }

  const colunas = [
    { chave: 'dataAbertura', titulo: 'Aberta em', renderizar: (o) => formatarDataHora(o.dataAbertura) },
    {
      chave: 'unidadeNumero', titulo: 'Unidade',
      renderizar: (o) => `${o.blocoNome}, ${o.unidadeNumero}`,
    },
    { chave: 'assunto', titulo: 'Assunto' },
    { chave: 'tipo', titulo: 'Tipo', renderizar: (o) => TIPOS[o.tipo] ?? o.tipo },
    { chave: 'situacao', titulo: 'Situação', renderizar: (o) => seloSituacao(o.situacao) },
    { chave: 'abertoPorNome', titulo: 'Aberta por' },
    {
      chave: 'acoes', titulo: 'Ações', ordenavel: false,
      renderizar: (o) => (
        <button type="button" className="botao-secundario" onClick={() => abrirDetalhe(o)}>
          {SITUACOES_ENCERRADAS.includes(o.situacao) ? 'Ver detalhes' : 'Responder'}
        </button>
      ),
    },
  ]

  return (
    <section>
      <h2>Ocorrências dos moradores</h2>
      <p className="descricao">
        Fila de reclamações, solicitações e defeitos abertos pelos moradores,
        com as abertas primeiro. Responder sempre exige um texto para quem abriu.
      </p>

      <label className="campo campo-filtro-situacao">
        <span>Situação</span>
        <select value={situacaoFiltro} onChange={(evento) => setSituacaoFiltro(evento.target.value)}>
          <option value="">Todas</option>
          <option value="ABERTA">Aberta</option>
          <option value="EM_ANALISE">Em análise</option>
          <option value="RESOLVIDA">Resolvida</option>
          <option value="RECUSADA">Recusada</option>
        </select>
      </label>

      <TabelaDados
        colunas={colunas}
        dados={fila}
        camposBusca={['assunto', 'abertoPorNome', 'unidadeNumero']}
        buscaPlaceholder="Buscar por assunto, unidade ou quem abriu..."
        mensagemVazia={carregando ? 'Carregando...' : 'Nenhuma ocorrência encontrada.'}
        mensagemErro={erroLista}
      />

      {ocorrenciaSelecionada && (
        <Modal titulo={ocorrenciaSelecionada.assunto} aoFechar={fecharDetalhe} className="modal-cartao-unidade">
          {carregandoDetalhe && <p className="descricao-curta">Carregando...</p>}
          {erroDetalhe && <p className="alerta-erro">{erroDetalhe}</p>}
          {detalhe && (
            <div>
              <p className="descricao-curta">
                {detalhe.blocoNome}, {detalhe.unidadeNumero} — aberta por {detalhe.abertoPorNome}
              </p>
              <p>{seloSituacao(detalhe.situacao)}</p>
              <p>{detalhe.descricao}</p>
              {detalhe.foto && (
                <img src={detalhe.foto} alt="Foto da ocorrência" className="foto-ocorrencia" />
              )}

              <h4>Histórico</h4>
              <ul className="lista-leituras-aviso">
                {detalhe.historico.map((h, indice) => (
                  <li key={indice}>
                    <strong>{seloSituacao(h.situacaoNova)}</strong>
                    {' '}por {h.responsavelNome} em {formatarDataHora(h.dataHora)}
                    {h.resposta && <p className="descricao-curta">{h.resposta}</p>}
                  </li>
                ))}
              </ul>

              {!SITUACOES_ENCERRADAS.includes(detalhe.situacao) && (
                <>
                  <h4>Responder</h4>
                  <form onSubmit={enviarResposta} className="formulario-inline">
                    <label className="campo">
                      <span>Nova situação</span>
                      <select value={novaSituacao} onChange={(e) => setNovaSituacao(e.target.value)}>
                        <option value="">Selecione</option>
                        {SITUACOES_RESPOSTA.map((s) => (<option key={s.valor} value={s.valor}>{s.rotulo}</option>))}
                      </select>
                    </label>
                    <label className="campo campo-descricao-ocorrencia">
                      <span>Resposta para o morador</span>
                      <textarea value={resposta} onChange={(e) => setResposta(e.target.value)} rows={3} placeholder="Explique o que foi feito ou por que foi recusada" />
                    </label>
                    <button type="submit" className="botao-primario" disabled={salvando}>
                      {salvando ? 'Enviando...' : 'Enviar resposta'}
                    </button>
                  </form>
                  {erroResposta && <p className="alerta-erro">{erroResposta}</p>}
                </>
              )}
            </div>
          )}
        </Modal>
      )}
    </section>
  )
}

export default FilaOcorrenciasMorador
