import { useState, useEffect, useCallback } from 'react'
import {
  convocarReuniao,
  listarReunioes,
  detalharReuniao,
  registrarPresencaReuniao,
  anexarAtaReuniao,
  listarBlocos,
  listarUnidades,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import Modal from './Modal.jsx'

const SITUACOES = {
  CONVOCADA: { rotulo: 'Convocada', classe: 'selo-inativo' },
  REALIZADA: { rotulo: 'Realizada', classe: 'selo-ativo' },
}

function seloSituacao(situacao) {
  const info = SITUACOES[situacao] ?? { rotulo: situacao, classe: 'selo-inativo' }
  return <span className={`selo ${info.classe}`}>{info.rotulo}</span>
}

function formatarData(data) {
  if (!data) return ''
  return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR')
}

function formatarHora(hora) {
  if (!hora) return ''
  return hora.slice(0, 5)
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
    leitor.onerror = () => reject(new Error('Não foi possível ler o arquivo da ata.'))
    leitor.readAsDataURL(arquivo)
  })
}

function Reunioes({ condominioId }) {
  const formVazio = { titulo: '', data: '', hora: '', local: '' }

  const [form, setForm] = useState(formVazio)
  const [pautaItens, setPautaItens] = useState([''])
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [inicioFiltro, setInicioFiltro] = useState('')
  const [fimFiltro, setFimFiltro] = useState('')
  const [reunioes, setReunioes] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroLista, setErroLista] = useState('')

  const [reuniaoSelecionada, setReuniaoSelecionada] = useState(null)
  const [detalhe, setDetalhe] = useState(null)
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false)
  const [erroDetalhe, setErroDetalhe] = useState('')

  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [formPresenca, setFormPresenca] = useState({ blocoId: '', unidadeId: '', compareceu: '' })
  const [erroPresenca, setErroPresenca] = useState('')
  const [salvandoPresenca, setSalvandoPresenca] = useState(false)

  const [ataTexto, setAtaTexto] = useState('')
  const [ataArquivo, setAtaArquivo] = useState(null)
  const [erroAta, setErroAta] = useState('')
  const [salvandoAta, setSalvandoAta] = useState(false)

  useEffect(() => {
    listarBlocos(condominioId).then(setBlocos).catch(() => setBlocos([]))
  }, [condominioId])

  useEffect(() => {
    if (!formPresenca.blocoId) { setUnidades([]); return }
    listarUnidades(condominioId, formPresenca.blocoId).then(setUnidades).catch(() => setUnidades([]))
  }, [condominioId, formPresenca.blocoId])

  const carregarReunioes = useCallback(() => {
    setCarregando(true)
    setErroLista('')
    listarReunioes(condominioId, inicioFiltro || null, fimFiltro || null)
      .then(setReunioes)
      .catch((e) => { setErroLista(e.message); setReunioes([]) })
      .finally(() => setCarregando(false))
  }, [condominioId, inicioFiltro, fimFiltro])

  useEffect(() => {
    carregarReunioes()
  }, [carregarReunioes])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErroForm('')
  }

  function atualizarPautaItem(indice, valor) {
    setPautaItens((anterior) => anterior.map((item, i) => (i === indice ? valor : item)))
    setErroForm('')
  }

  function adicionarItemPauta() {
    setPautaItens((anterior) => [...anterior, ''])
  }

  function removerItemPauta(indice) {
    setPautaItens((anterior) => anterior.filter((_, i) => i !== indice))
  }

  async function convocar(evento) {
    evento.preventDefault()
    setErroForm('')
    if (!form.titulo.trim()) { setErroForm('Informe o título da reunião.'); return }
    if (!form.data) { setErroForm('Informe a data da reunião.'); return }
    if (!form.hora) { setErroForm('Informe a hora da reunião.'); return }
    if (!form.local.trim()) { setErroForm('Informe o local da reunião.'); return }

    const pauta = pautaItens.map((item) => item.trim()).filter((item) => item !== '')
    if (pauta.length === 0) { setErroForm('Informe a pauta com pelo menos um assunto.'); return }

    setSalvando(true)
    try {
      await convocarReuniao(condominioId, {
        titulo: form.titulo.trim(),
        data: form.data,
        hora: form.hora,
        local: form.local.trim(),
        pauta,
      })
      setForm(formVazio)
      setPautaItens([''])
      carregarReunioes()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function abrirDetalhe(reuniao) {
    setReuniaoSelecionada(reuniao)
    setDetalhe(null)
    setErroDetalhe('')
    setFormPresenca({ blocoId: '', unidadeId: '', compareceu: '' })
    setErroPresenca('')
    setAtaTexto('')
    setAtaArquivo(null)
    setErroAta('')
    setCarregandoDetalhe(true)
    try {
      setDetalhe(await detalharReuniao(condominioId, reuniao.id))
    } catch (e) {
      setErroDetalhe(e.message)
    } finally {
      setCarregandoDetalhe(false)
    }
  }

  function fecharDetalhe() {
    setReuniaoSelecionada(null)
    setDetalhe(null)
    setErroDetalhe('')
  }

  function atualizarCampoPresenca(evento) {
    const { name, value } = evento.target
    setFormPresenca((anterior) => {
      if (name === 'blocoId') return { ...anterior, blocoId: value, unidadeId: '' }
      return { ...anterior, [name]: value }
    })
    setErroPresenca('')
  }

  async function registrarPresenca(evento) {
    evento.preventDefault()
    setErroPresenca('')
    if (!formPresenca.unidadeId) { setErroPresenca('Selecione a unidade.'); return }
    if (!formPresenca.compareceu.trim()) { setErroPresenca('Informe quem compareceu pela unidade.'); return }

    setSalvandoPresenca(true)
    try {
      await registrarPresencaReuniao(condominioId, reuniaoSelecionada.id, {
        unidadeId: Number(formPresenca.unidadeId),
        compareceu: formPresenca.compareceu.trim(),
      })
      setFormPresenca({ blocoId: '', unidadeId: '', compareceu: '' })
      setDetalhe(await detalharReuniao(condominioId, reuniaoSelecionada.id))
      carregarReunioes()
    } catch (e) {
      setErroPresenca(e.message)
    } finally {
      setSalvandoPresenca(false)
    }
  }

  async function selecionarArquivoAta(evento) {
    const arquivo = evento.target.files?.[0]
    if (!arquivo) { setAtaArquivo(null); return }
    try {
      setAtaArquivo(await lerArquivoComoBase64(arquivo))
      setErroAta('')
    } catch (e) {
      setErroAta(e.message)
      evento.target.value = ''
      setAtaArquivo(null)
    }
  }

  async function anexarAta(evento) {
    evento.preventDefault()
    setErroAta('')
    if (!ataTexto.trim()) { setErroAta('Informe o texto da ata com as deliberações.'); return }

    setSalvandoAta(true)
    try {
      await anexarAtaReuniao(condominioId, reuniaoSelecionada.id, {
        texto: ataTexto.trim(),
        arquivo: ataArquivo,
      })
      setAtaTexto('')
      setAtaArquivo(null)
      setDetalhe(await detalharReuniao(condominioId, reuniaoSelecionada.id))
      carregarReunioes()
    } catch (e) {
      setErroAta(e.message)
    } finally {
      setSalvandoAta(false)
    }
  }

  const colunas = [
    { chave: 'data', titulo: 'Data', renderizar: (r) => formatarData(r.data) },
    { chave: 'hora', titulo: 'Hora', renderizar: (r) => formatarHora(r.hora) },
    { chave: 'titulo', titulo: 'Título' },
    { chave: 'local', titulo: 'Local' },
    { chave: 'situacao', titulo: 'Situação', renderizar: (r) => seloSituacao(r.situacao) },
    { chave: 'itensPauta', titulo: 'Itens da pauta' },
    { chave: 'unidadesPresentes', titulo: 'Unidades presentes' },
    {
      chave: 'acoes', titulo: 'Ações', ordenavel: false,
      renderizar: (r) => (
        <button type="button" className="botao-secundario" onClick={() => abrirDetalhe(r)}>
          Ver detalhes
        </button>
      ),
    },
  ]

  return (
    <section>
      <h2>Reuniões e assembleias</h2>
      <p className="descricao">
        Convoque a assembleia com data, hora, local e pauta. No dia, registre a
        presença por unidade e, depois de realizada, anexe a ata com as deliberações.
      </p>

      <h3 className="subtitulo-secao">Convocar reunião</h3>
      <form onSubmit={convocar} className="formulario-inline">
        <label className="campo">
          <span>Título</span>
          <input name="titulo" value={form.titulo} onChange={atualizarCampo} placeholder="Ex: Assembleia geral ordinária" required />
        </label>
        <label className="campo">
          <span>Data</span>
          <input type="date" name="data" value={form.data} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Hora</span>
          <input type="time" name="hora" value={form.hora} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Local</span>
          <input name="local" value={form.local} onChange={atualizarCampo} placeholder="Ex: Salão de festas" required />
        </label>

        <div className="campo campo-descricao-ocorrencia">
          <span>Pauta</span>
          {pautaItens.map((item, indice) => (
            <div key={indice} className="item-pauta">
              <input
                value={item}
                onChange={(e) => atualizarPautaItem(indice, e.target.value)}
                placeholder={`Assunto ${indice + 1}`}
              />
              {pautaItens.length > 1 && (
                <button type="button" className="botao-secundario" onClick={() => removerItemPauta(indice)}>
                  Remover
                </button>
              )}
            </div>
          ))}
          <button type="button" className="botao-secundario" onClick={adicionarItemPauta}>
            Adicionar item
          </button>
        </div>

        <button type="submit" className="botao-primario" disabled={salvando}>
          {salvando ? 'Convocando...' : 'Convocar reunião'}
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}

      <h3 className="subtitulo-secao">Histórico de reuniões</h3>
      <div className="formulario-inline">
        <label className="campo">
          <span>De</span>
          <input type="date" value={inicioFiltro} onChange={(e) => setInicioFiltro(e.target.value)} />
        </label>
        <label className="campo">
          <span>Até</span>
          <input type="date" value={fimFiltro} onChange={(e) => setFimFiltro(e.target.value)} />
        </label>
      </div>
      <TabelaDados
        colunas={colunas}
        dados={reunioes}
        camposBusca={['titulo', 'local']}
        buscaPlaceholder="Buscar por título ou local..."
        mensagemVazia={carregando ? 'Carregando...' : 'Nenhuma reunião registrada ainda.'}
        mensagemErro={erroLista}
      />

      {reuniaoSelecionada && (
        <Modal titulo={reuniaoSelecionada.titulo} aoFechar={fecharDetalhe} className="modal-cartao-unidade">
          {carregandoDetalhe && <p className="descricao-curta">Carregando...</p>}
          {erroDetalhe && <p className="alerta-erro">{erroDetalhe}</p>}
          {detalhe && (
            <div>
              <p>
                {seloSituacao(detalhe.situacao)} — {formatarData(detalhe.data)}, {formatarHora(detalhe.hora)} —{' '}
                {detalhe.local}
              </p>
              <p className="descricao-curta">
                Convocada por {detalhe.convocadaPorNome} em {formatarDataHora(detalhe.dataPublicacao)}
              </p>

              <h4>Pauta</h4>
              <ol>
                {detalhe.pauta.map((item) => (
                  <li key={item.ordem}>{item.descricao}</li>
                ))}
              </ol>

              <h4>Presença ({detalhe.unidadesPresentes} de {detalhe.unidadesDoCondominio} unidades)</h4>
              {detalhe.presencas.length === 0 ? (
                <p className="descricao-curta">Nenhuma presença registrada ainda.</p>
              ) : (
                <ul className="lista-leituras-aviso">
                  {detalhe.presencas.map((p) => (
                    <li key={p.id}>
                      {p.blocoNome}, {p.unidadeNumero} — {p.compareceu}
                    </li>
                  ))}
                </ul>
              )}

              {detalhe.situacao === 'CONVOCADA' && (
                <>
                  <h4>Registrar presença</h4>
                  <form onSubmit={registrarPresenca} className="formulario-inline">
                    <label className="campo">
                      <span>Bloco</span>
                      <select name="blocoId" value={formPresenca.blocoId} onChange={atualizarCampoPresenca}>
                        <option value="">Selecione</option>
                        {blocos.map((b) => (<option key={b.id} value={b.id}>{b.nome}</option>))}
                      </select>
                    </label>
                    <label className="campo">
                      <span>Unidade</span>
                      <select
                        name="unidadeId"
                        value={formPresenca.unidadeId}
                        onChange={atualizarCampoPresenca}
                        disabled={!formPresenca.blocoId}
                      >
                        <option value="">Selecione</option>
                        {unidades.map((u) => (<option key={u.id} value={u.id}>{u.numero}</option>))}
                      </select>
                    </label>
                    <label className="campo">
                      <span>Quem compareceu</span>
                      <input
                        name="compareceu"
                        value={formPresenca.compareceu}
                        onChange={atualizarCampoPresenca}
                        placeholder="Ex: Maria Silva"
                      />
                    </label>
                    <button type="submit" className="botao-primario" disabled={salvandoPresenca}>
                      {salvandoPresenca ? 'Registrando...' : 'Registrar presença'}
                    </button>
                  </form>
                  {erroPresenca && <p className="alerta-erro">{erroPresenca}</p>}
                </>
              )}

              <h4>Ata</h4>
              {detalhe.ata ? (
                <div>
                  <p>{detalhe.ata.texto}</p>
                  <p className="descricao-curta">
                    Anexada por {detalhe.ata.anexadaPorNome} em {formatarDataHora(detalhe.ata.dataAnexo)}
                  </p>
                  {detalhe.ata.temArquivo && (
                    <a href={detalhe.ata.arquivo} download={`ata-${detalhe.id}`}>Baixar anexo</a>
                  )}
                </div>
              ) : detalhe.situacao === 'CONVOCADA' ? (
                <form onSubmit={anexarAta} className="formulario-inline">
                  <label className="campo campo-descricao-ocorrencia">
                    <span>Texto da ata</span>
                    <textarea
                      value={ataTexto}
                      onChange={(e) => { setAtaTexto(e.target.value); setErroAta('') }}
                      rows={3}
                      placeholder="Descreva as deliberações da reunião"
                    />
                  </label>
                  <label className="campo">
                    <span>Anexo (opcional)</span>
                    <input type="file" onChange={selecionarArquivoAta} />
                  </label>
                  <button type="submit" className="botao-primario" disabled={salvandoAta}>
                    {salvandoAta ? 'Anexando...' : 'Anexar ata'}
                  </button>
                </form>
              ) : (
                <p className="descricao-curta">Ata ainda não disponível.</p>
              )}
              {erroAta && <p className="alerta-erro">{erroAta}</p>}
            </div>
          )}
        </Modal>
      )}
    </section>
  )
}

export default Reunioes
