import { useState, useEffect, useCallback } from 'react'
import { listarMeusAvisos, confirmarLeituraAviso } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

function rotuloAlcance(valor) {
  if (valor === 'CONDOMINIO') return 'Todo o condomínio'
  if (valor === 'BLOCO') return 'Bloco'
  if (valor === 'UNIDADE') return 'Unidade'
  return valor
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

function MeusAvisos({ condominioId }) {
  const [avisos, setAvisos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [confirmandoId, setConfirmandoId] = useState(null)
  const [erroConfirmacao, setErroConfirmacao] = useState(null)

  const carregar = useCallback(() => {
    setCarregando(true)
    setErro('')
    listarMeusAvisos(condominioId)
      .then(setAvisos)
      .catch((e) => { setErro(e.message); setAvisos([]) })
      .finally(() => setCarregando(false))
  }, [condominioId])

  useEffect(() => {
    carregar()
  }, [carregar])

  async function confirmarLeitura(aviso) {
    setErroConfirmacao(null)
    setConfirmandoId(aviso.id)
    try {
      await confirmarLeituraAviso(condominioId, aviso.id)
      carregar()
    } catch (e) {
      setErroConfirmacao({ id: aviso.id, mensagem: e.message })
    } finally {
      setConfirmandoId(null)
    }
  }

  const colunas = [
    { chave: 'dataCriacao', titulo: 'Publicado em', renderizar: (a) => formatarDataHora(a.dataCriacao) },
    { chave: 'titulo', titulo: 'Título' },
    { chave: 'texto', titulo: 'Texto', ordenavel: false, renderizar: (a) => <p>{a.texto}</p> },
    { chave: 'alcance', titulo: 'Alcance', renderizar: (a) => rotuloAlcance(a.alcance) },
    { chave: 'dataValidade', titulo: 'Válido até', renderizar: (a) => formatarData(a.dataValidade) },
    { chave: 'autorNome', titulo: 'Autor' },
    {
      chave: 'lido', titulo: 'Leitura', ordenavel: false,
      renderizar: (a) =>
        a.lido ? (
          <span className="selo selo-ativo">Lido em {formatarDataHora(a.dataLeitura)}</span>
        ) : (
          <button
            type="button"
            className="botao-primario"
            onClick={() => confirmarLeitura(a)}
            disabled={confirmandoId === a.id}
          >
            {confirmandoId === a.id ? 'Confirmando...' : 'Confirmar leitura'}
          </button>
        ),
    },
  ]

  return (
    <section>
      <h2>Avisos</h2>
      <p className="descricao">
        Avisos do condomínio, do seu bloco ou da sua unidade. Confirme a leitura para
        que o síndico saiba que você viu. Avisos vencidos somem desta lista.
      </p>

      {erroConfirmacao && <p className="alerta-erro">{erroConfirmacao.mensagem}</p>}

      <TabelaDados
        colunas={colunas}
        dados={avisos}
        camposBusca={['titulo', 'autorNome']}
        buscaPlaceholder="Buscar por título ou autor..."
        mensagemVazia={carregando ? 'Carregando...' : 'Nenhum aviso no momento.'}
        mensagemErro={erro}
      />
    </section>
  )
}

export default MeusAvisos
