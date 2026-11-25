import { useState, useEffect, useCallback } from 'react'
import {
  abrirOcorrenciaMorador,
  listarOcorrenciasDaUnidade,
  detalharOcorrenciaDaUnidade,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import Modal from './Modal.jsx'

const TIPOS = [
  { valor: 'RECLAMACAO', rotulo: 'Reclamação' },
  { valor: 'SOLICITACAO', rotulo: 'Solicitação' },
  { valor: 'DEFEITO', rotulo: 'Defeito' },
]

const SITUACOES = {
  ABERTA: { rotulo: 'Aberta', classe: 'selo-inativo' },
  EM_ANALISE: { rotulo: 'Em análise', classe: 'selo-inativo' },
  RESOLVIDA: { rotulo: 'Resolvida', classe: 'selo-ativo' },
  RECUSADA: { rotulo: 'Recusada', classe: 'selo-vencido' },
}

function rotuloTipo(valor) {
  return TIPOS.find((t) => t.valor === valor)?.rotulo ?? valor
}

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

function lerArquivoComoBase64(arquivo) {
  return new Promise((resolve, reject) => {
    const leitor = new FileReader()
    leitor.onload = () => resolve(leitor.result)
    leitor.onerror = () => reject(new Error('Não foi possível ler o arquivo da foto.'))
    leitor.readAsDataURL(arquivo)
  })
}

function MinhasOcorrencias({ condominioId, blocoId, unidadeId }) {
  const formVazio = { assunto: '', tipo: '', descricao: '' }

  const [form, setForm] = useState(formVazio)
  const [foto, setFoto] = useState(null)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [ocorrencias, setOcorrencias] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroLista, setErroLista] = useState('')

  const [ocorrenciaSelecionada, setOcorrenciaSelecionada] = useState(null)
  const [detalhe, setDetalhe] = useState(null)
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false)
  const [erroDetalhe, setErroDetalhe] = useState('')

  const carregar = useCallback(() => {
    if (!blocoId || !unidadeId) {
      setCarregando(false)
      return
    }
    setCarregando(true)
    setErroLista('')
    listarOcorrenciasDaUnidade(condominioId, blocoId, unidadeId)
      .then(setOcorrencias)
      .catch((e) => { setErroLista(e.message); setOcorrencias([]) })
      .finally(() => setCarregando(false))
  }, [condominioId, blocoId, unidadeId])

  useEffect(() => {
    carregar()
  }, [carregar])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErroForm('')
  }

  async function selecionarFoto(evento) {
    const arquivo = evento.target.files?.[0]
    if (!arquivo) { setFoto(null); return }
    try {
      setFoto(await lerArquivoComoBase64(arquivo))
      setErroForm('')
    } catch (e) {
      setErroForm(e.message)
      evento.target.value = ''
      setFoto(null)
    }
  }

  async function abrir(evento) {
    evento.preventDefault()
    setErroForm('')
    if (!form.assunto.trim()) { setErroForm('Informe o assunto da ocorrência.'); return }
    if (!form.tipo) { setErroForm('Informe o tipo da ocorrência.'); return }
    if (!form.descricao.trim()) { setErroForm('Informe a descrição da ocorrência.'); return }

    setSalvando(true)
    try {
      await abrirOcorrenciaMorador(condominioId, blocoId, unidadeId, {
        assunto: form.assunto.trim(),
        tipo: form.tipo,
        descricao: form.descricao.trim(),
        foto,
      })
      setForm(formVazio)
      setFoto(null)
      carregar()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function abrirDetalhe(ocorrencia) {
    setOcorrenciaSelecionada(ocorrencia)
    setDetalhe(null)
    setErroDetalhe('')
    setCarregandoDetalhe(true)
    try {
      setDetalhe(await detalharOcorrenciaDaUnidade(condominioId, blocoId, unidadeId, ocorrencia.id))
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

  const colunas = [
    { chave: 'dataAbertura', titulo: 'Aberta em', renderizar: (o) => formatarDataHora(o.dataAbertura) },
    { chave: 'assunto', titulo: 'Assunto' },
    { chave: 'tipo', titulo: 'Tipo', renderizar: (o) => rotuloTipo(o.tipo) },
    { chave: 'situacao', titulo: 'Situação', renderizar: (o) => seloSituacao(o.situacao) },
    { chave: 'abertoPorNome', titulo: 'Aberta por' },
    {
      chave: 'ultimaResposta', titulo: 'Última resposta', ordenavel: false,
      renderizar: (o) => (o.ultimaResposta ? <p>{o.ultimaResposta}</p> : <p className="descricao-curta">Aguardando resposta.</p>),
    },
    {
      chave: 'acoes', titulo: 'Ações', ordenavel: false,
      renderizar: (o) => (
        <button type="button" className="botao-secundario" onClick={() => abrirDetalhe(o)}>
          Ver detalhes
        </button>
      ),
    },
  ]

  if (!blocoId || !unidadeId) {
    return (
      <section>
        <h2>Minhas ocorrências</h2>
        <p className="aviso-vazio">
          Não foi possível identificar a sua unidade. Se você acabou de ser
          cadastrado, peça ao síndico para registrar a sua entrada.
        </p>
      </section>
    )
  }

  return (
    <section>
      <h2>Minhas ocorrências</h2>
      <p className="descricao">
        Registre reclamações, solicitações ou defeitos. Toda ocorrência fica
        documentada, com data, quem respondeu e o histórico completo.
      </p>

      <h3 className="subtitulo-secao">Nova ocorrência</h3>
      <form onSubmit={abrir} className="formulario-inline">
        <label className="campo">
          <span>Assunto</span>
          <input name="assunto" value={form.assunto} onChange={atualizarCampo} placeholder="Ex: Vazamento na garagem" required />
        </label>
        <label className="campo">
          <span>Tipo</span>
          <select name="tipo" value={form.tipo} onChange={atualizarCampo} required>
            <option value="">Selecione</option>
            {TIPOS.map((t) => (<option key={t.valor} value={t.valor}>{t.rotulo}</option>))}
          </select>
        </label>
        <label className="campo campo-descricao-ocorrencia">
          <span>Descrição</span>
          <textarea name="descricao" value={form.descricao} onChange={atualizarCampo} rows={3} placeholder="Descreva o que está acontecendo" required />
        </label>
        <label className="campo">
          <span>Foto (opcional)</span>
          <input type="file" accept="image/*" onChange={selecionarFoto} />
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          {salvando ? 'Registrando...' : 'Registrar ocorrência'}
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}

      <h3 className="subtitulo-secao">Histórico</h3>
      <TabelaDados
        colunas={colunas}
        dados={ocorrencias}
        camposBusca={['assunto', 'abertoPorNome']}
        buscaPlaceholder="Buscar por assunto ou quem abriu..."
        mensagemVazia={carregando ? 'Carregando...' : 'Nenhuma ocorrência registrada ainda.'}
        mensagemErro={erroLista}
      />

      {ocorrenciaSelecionada && (
        <Modal titulo={ocorrenciaSelecionada.assunto} aoFechar={fecharDetalhe} className="modal-cartao-unidade">
          {carregandoDetalhe && <p className="descricao-curta">Carregando...</p>}
          {erroDetalhe && <p className="alerta-erro">{erroDetalhe}</p>}
          {detalhe && (
            <div>
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
            </div>
          )}
        </Modal>
      )}
    </section>
  )
}

export default MinhasOcorrencias
