import { useState, useEffect, useCallback } from 'react'
import {
  listarBlocos,
  listarUnidades,
  criarAviso,
  listarHistoricoAvisos,
  listarLeiturasAviso,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import Modal from './Modal.jsx'

const ALCANCES = [
  { valor: 'CONDOMINIO', rotulo: 'Todo o condomínio' },
  { valor: 'BLOCO', rotulo: 'Um bloco' },
  { valor: 'UNIDADE', rotulo: 'Uma unidade' },
]

function rotuloAlcance(valor) {
  return ALCANCES.find((a) => a.valor === valor)?.rotulo ?? valor
}

function formatarData(data) {
  if (!data) return ''
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}

function formatarDataHora(dataHora) {
  if (!dataHora) return ''
  return new Date(dataHora).toLocaleString('pt-BR', {
    day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit',
  })
}

function hoje() {
  return new Date().toISOString().slice(0, 10)
}

function Avisos({ condominioId }) {
  const formVazio = { titulo: '', texto: '', alcance: 'CONDOMINIO', blocoId: '', unidadeId: '', dataValidade: '' }

  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [historico, setHistorico] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroLista, setErroLista] = useState('')

  const [avisoSelecionado, setAvisoSelecionado] = useState(null)
  const [leituras, setLeituras] = useState(null)
  const [carregandoLeituras, setCarregandoLeituras] = useState(false)
  const [erroLeituras, setErroLeituras] = useState('')

  useEffect(() => {
    listarBlocos(condominioId).then(setBlocos).catch(() => setBlocos([]))
  }, [condominioId])

  useEffect(() => {
    if (!form.blocoId) { setUnidades([]); return }
    listarUnidades(condominioId, form.blocoId).then(setUnidades).catch(() => setUnidades([]))
  }, [condominioId, form.blocoId])

  const carregarHistorico = useCallback(() => {
    setCarregando(true)
    setErroLista('')
    listarHistoricoAvisos(condominioId)
      .then(setHistorico)
      .catch((e) => { setErroLista(e.message); setHistorico([]) })
      .finally(() => setCarregando(false))
  }, [condominioId])

  useEffect(() => {
    carregarHistorico()
  }, [carregarHistorico])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => {
      if (name === 'alcance') return { ...anterior, alcance: value, blocoId: '', unidadeId: '' }
      if (name === 'blocoId') return { ...anterior, blocoId: value, unidadeId: '' }
      return { ...anterior, [name]: value }
    })
    setErroForm('')
  }

  async function criar(evento) {
    evento.preventDefault()
    setErroForm('')
    if (!form.titulo.trim()) { setErroForm('Informe o título do aviso.'); return }
    if (!form.texto.trim()) { setErroForm('Informe o texto do aviso.'); return }
    if (form.alcance === 'BLOCO' && !form.blocoId) { setErroForm('Selecione o bloco do aviso.'); return }
    if (form.alcance === 'UNIDADE' && !form.unidadeId) { setErroForm('Selecione a unidade do aviso.'); return }
    if (!form.dataValidade) { setErroForm('Informe a validade do aviso.'); return }

    setSalvando(true)
    try {
      await criarAviso(condominioId, {
        titulo: form.titulo.trim(),
        texto: form.texto.trim(),
        alcance: form.alcance,
        blocoId: form.alcance === 'BLOCO' || form.alcance === 'UNIDADE' ? Number(form.blocoId) : null,
        unidadeId: form.alcance === 'UNIDADE' ? Number(form.unidadeId) : null,
        dataValidade: form.dataValidade,
      })
      setForm(formVazio)
      carregarHistorico()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function abrirLeituras(aviso) {
    setAvisoSelecionado(aviso)
    setLeituras(null)
    setErroLeituras('')
    setCarregandoLeituras(true)
    try {
      setLeituras(await listarLeiturasAviso(condominioId, aviso.id))
    } catch (e) {
      setErroLeituras(e.message)
    } finally {
      setCarregandoLeituras(false)
    }
  }

  function fecharLeituras() {
    setAvisoSelecionado(null)
    setLeituras(null)
    setErroLeituras('')
  }

  const colunas = [
    { chave: 'dataCriacao', titulo: 'Criado em', renderizar: (a) => formatarDataHora(a.dataCriacao) },
    { chave: 'titulo', titulo: 'Título' },
    {
      chave: 'alcance', titulo: 'Alcance',
      renderizar: (a) => {
        if (a.alcance === 'UNIDADE') return `${a.blocoNome}, ${a.unidadeNumero}`
        if (a.alcance === 'BLOCO') return a.blocoNome
        return rotuloAlcance(a.alcance)
      },
    },
    { chave: 'dataValidade', titulo: 'Validade', renderizar: (a) => formatarData(a.dataValidade) },
    {
      chave: 'vencido', titulo: 'Situação',
      renderizar: (a) => (
        a.vencido
          ? <span className="selo selo-vencido">Vencido</span>
          : <span className="selo selo-ativo">Vigente</span>
      ),
    },
    { chave: 'autorNome', titulo: 'Autor' },
    {
      chave: 'acoes', titulo: 'Ações', ordenavel: false,
      renderizar: (a) => (
        <button type="button" className="botao-secundario" onClick={() => abrirLeituras(a)}>
          Ver leituras
        </button>
      ),
    },
  ]

  return (
    <section>
      <h2>Avisos</h2>
      <p className="descricao">
        Avise todo o condomínio, um bloco ou uma unidade específica. O aviso vai para
        quem mora na unidade — o proprietário só recebe se a unidade estiver vazia.
      </p>

      <h3 className="subtitulo-secao">Novo aviso</h3>
      <form onSubmit={criar} className="formulario-inline">
        <label className="campo">
          <span>Título</span>
          <input name="titulo" value={form.titulo} onChange={atualizarCampo} placeholder="Ex: Manutenção do elevador" required />
        </label>
        <label className="campo">
          <span>Alcance</span>
          <select name="alcance" value={form.alcance} onChange={atualizarCampo}>
            {ALCANCES.map((a) => (<option key={a.valor} value={a.valor}>{a.rotulo}</option>))}
          </select>
        </label>
        {(form.alcance === 'BLOCO' || form.alcance === 'UNIDADE') && (
          <label className="campo">
            <span>Bloco</span>
            <select name="blocoId" value={form.blocoId} onChange={atualizarCampo}>
              <option value="">Selecione</option>
              {blocos.map((b) => (<option key={b.id} value={b.id}>{b.nome}</option>))}
            </select>
          </label>
        )}
        {form.alcance === 'UNIDADE' && (
          <label className="campo">
            <span>Unidade</span>
            <select name="unidadeId" value={form.unidadeId} onChange={atualizarCampo} disabled={!form.blocoId}>
              <option value="">Selecione</option>
              {unidades.map((u) => (<option key={u.id} value={u.id}>{u.numero} (andar {u.andar})</option>))}
            </select>
          </label>
        )}
        <label className="campo">
          <span>Validade</span>
          <input type="date" name="dataValidade" value={form.dataValidade} onChange={atualizarCampo} min={hoje()} required />
        </label>
        <label className="campo campo-texto-aviso">
          <span>Texto</span>
          <textarea name="texto" value={form.texto} onChange={atualizarCampo} rows={3} placeholder="O que o condomínio precisa saber" required />
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          {salvando ? 'Publicando...' : 'Publicar aviso'}
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}

      <h3 className="subtitulo-secao">Histórico de avisos</h3>
      <TabelaDados
        colunas={colunas}
        dados={historico}
        camposBusca={['titulo', 'autorNome']}
        buscaPlaceholder="Buscar por título ou autor..."
        mensagemVazia={carregando ? 'Carregando...' : 'Nenhum aviso publicado ainda.'}
        mensagemErro={erroLista}
      />

      {avisoSelecionado && (
        <Modal titulo={`Leituras: ${avisoSelecionado.titulo}`} aoFechar={fecharLeituras}>
          {carregandoLeituras && <p className="descricao-curta">Carregando...</p>}
          {erroLeituras && <p className="alerta-erro">{erroLeituras}</p>}
          {leituras && (
            <div className="painel-leituras-aviso">
              <p className="descricao-curta">
                {leituras.totalLeram} de {leituras.totalDestinatarios} destinatário(s) leram este aviso.
              </p>

              <h4>Leram ({leituras.totalLeram})</h4>
              {leituras.leram.length === 0 ? (
                <p className="descricao-curta">Ninguém leu este aviso ainda.</p>
              ) : (
                <ul className="lista-leituras-aviso">
                  {leituras.leram.map((l) => (
                    <li key={l.usuarioId}>
                      <strong>{l.nome}</strong>
                      {l.unidadeId ? ` — ${l.blocoNome}, ${l.unidadeNumero}` : ''}
                      {' '}<span className="descricao-curta">({l.papel === 'PROPRIETARIO' ? 'proprietário' : 'morador'})</span>
                      <p className="descricao-curta">Lido em {formatarDataHora(l.dataLeitura)}</p>
                    </li>
                  ))}
                </ul>
              )}

              <h4>Não leram ({leituras.totalNaoLeram})</h4>
              {leituras.naoLeram.length === 0 ? (
                <p className="descricao-curta">Todos os destinatários já leram.</p>
              ) : (
                <ul className="lista-leituras-aviso">
                  {leituras.naoLeram.map((l) => (
                    <li key={l.usuarioId}>
                      <strong>{l.nome}</strong>
                      {l.unidadeId ? ` — ${l.blocoNome}, ${l.unidadeNumero}` : ''}
                      {' '}<span className="descricao-curta">({l.papel === 'PROPRIETARIO' ? 'proprietário' : 'morador'})</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </Modal>
      )}
    </section>
  )
}

export default Avisos
