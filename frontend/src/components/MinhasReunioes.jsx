import { useState, useCallback, useEffect } from 'react'
import { listarReunioes, detalharReuniao } from '../services/api.js'
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

function MinhasReunioes({ condominioId }) {
  const [inicioFiltro, setInicioFiltro] = useState('')
  const [fimFiltro, setFimFiltro] = useState('')
  const [reunioes, setReunioes] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erroLista, setErroLista] = useState('')

  const [reuniaoSelecionada, setReuniaoSelecionada] = useState(null)
  const [detalhe, setDetalhe] = useState(null)
  const [carregandoDetalhe, setCarregandoDetalhe] = useState(false)
  const [erroDetalhe, setErroDetalhe] = useState('')

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

  async function abrirDetalhe(reuniao) {
    setReuniaoSelecionada(reuniao)
    setDetalhe(null)
    setErroDetalhe('')
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

  const colunas = [
    { chave: 'data', titulo: 'Data', renderizar: (r) => formatarData(r.data) },
    { chave: 'hora', titulo: 'Hora', renderizar: (r) => formatarHora(r.hora) },
    { chave: 'titulo', titulo: 'Título' },
    { chave: 'local', titulo: 'Local' },
    { chave: 'situacao', titulo: 'Situação', renderizar: (r) => seloSituacao(r.situacao) },
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
      <h2>Reuniões</h2>
      <p className="descricao">
        Convocações da assembleia do condomínio, com a pauta, quem compareceu
        por unidade e a ata depois que a reunião acontece.
      </p>

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
        mensagemVazia={carregando ? 'Carregando...' : 'Nenhuma reunião convocada ainda.'}
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
              ) : (
                <p className="descricao-curta">Ata ainda não disponível.</p>
              )}
            </div>
          )}
        </Modal>
      )}
    </section>
  )
}

export default MinhasReunioes
