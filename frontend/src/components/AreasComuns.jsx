import { useState, useEffect, useCallback } from 'react'
import {
  cadastrarAreaComum,
  listarAreasComuns,
  listarReservasPendentes,
  aprovarReserva,
  recusarReserva,
} from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'
import CalendarioReservas from './CalendarioReservas.jsx'

function formatarData(data) {
  if (!data) return ''
  return new Date(`${data}T00:00:00`).toLocaleDateString('pt-BR')
}

function formatarHora(hora) {
  if (!hora) return ''
  return hora.slice(0, 5)
}

function AreasComuns({ condominioId }) {
  const formVazio = { nome: '', capacidade: '', horarioAbertura: '', horarioFechamento: '', exigeAprovacao: false }

  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [areas, setAreas] = useState([])
  const [carregandoAreas, setCarregandoAreas] = useState(true)
  const [erroAreas, setErroAreas] = useState('')

  const [pendentes, setPendentes] = useState([])
  const [carregandoPendentes, setCarregandoPendentes] = useState(true)
  const [erroPendentes, setErroPendentes] = useState('')

  const [recusandoId, setRecusandoId] = useState(null)
  const [motivoRecusa, setMotivoRecusa] = useState('')
  const [salvandoAcaoId, setSalvandoAcaoId] = useState(null)
  const [erroAcao, setErroAcao] = useState(null)

  const carregarAreas = useCallback(() => {
    setCarregandoAreas(true)
    setErroAreas('')
    listarAreasComuns(condominioId)
      .then(setAreas)
      .catch((e) => { setErroAreas(e.message); setAreas([]) })
      .finally(() => setCarregandoAreas(false))
  }, [condominioId])

  const carregarPendentes = useCallback(() => {
    setCarregandoPendentes(true)
    setErroPendentes('')
    listarReservasPendentes(condominioId)
      .then(setPendentes)
      .catch((e) => { setErroPendentes(e.message); setPendentes([]) })
      .finally(() => setCarregandoPendentes(false))
  }, [condominioId])

  useEffect(() => { carregarAreas() }, [carregarAreas])
  useEffect(() => { carregarPendentes() }, [carregarPendentes])

  function atualizarCampo(evento) {
    const { name, value, type, checked } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: type === 'checkbox' ? checked : value }))
    setErroForm('')
  }

  async function cadastrar(evento) {
    evento.preventDefault()
    setErroForm('')
    if (!form.nome.trim()) { setErroForm('Informe o nome da área comum.'); return }
    if (!form.capacidade) { setErroForm('Informe a capacidade da área comum.'); return }
    if (!form.horarioAbertura || !form.horarioFechamento) {
      setErroForm('Informe o horário de funcionamento.')
      return
    }

    setSalvando(true)
    try {
      await cadastrarAreaComum(condominioId, {
        nome: form.nome.trim(),
        capacidade: Number(form.capacidade),
        horarioAbertura: form.horarioAbertura,
        horarioFechamento: form.horarioFechamento,
        exigeAprovacao: form.exigeAprovacao,
      })
      setForm(formVazio)
      carregarAreas()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function aprovar(reserva) {
    setErroAcao(null)
    setSalvandoAcaoId(reserva.id)
    try {
      await aprovarReserva(condominioId, reserva.id)
      carregarPendentes()
    } catch (e) {
      setErroAcao({ id: reserva.id, mensagem: e.message })
    } finally {
      setSalvandoAcaoId(null)
    }
  }

  function abrirRecusa(reserva) {
    setRecusandoId(reserva.id)
    setMotivoRecusa('')
    setErroAcao(null)
  }

  function cancelarRecusa() {
    setRecusandoId(null)
    setMotivoRecusa('')
  }

  async function confirmarRecusa(reserva) {
    if (!motivoRecusa.trim()) {
      setErroAcao({ id: reserva.id, mensagem: 'Informe o motivo da recusa.' })
      return
    }
    setErroAcao(null)
    setSalvandoAcaoId(reserva.id)
    try {
      await recusarReserva(condominioId, reserva.id, motivoRecusa.trim())
      setRecusandoId(null)
      setMotivoRecusa('')
      carregarPendentes()
    } catch (e) {
      setErroAcao({ id: reserva.id, mensagem: e.message })
    } finally {
      setSalvandoAcaoId(null)
    }
  }

  const colunasAreas = [
    { chave: 'nome', titulo: 'Nome' },
    { chave: 'capacidade', titulo: 'Capacidade' },
    {
      chave: 'horarioAbertura', titulo: 'Funcionamento',
      renderizar: (a) => `${formatarHora(a.horarioAbertura)} às ${formatarHora(a.horarioFechamento)}`,
    },
    {
      chave: 'exigeAprovacao', titulo: 'Aprovação',
      renderizar: (a) => (a.exigeAprovacao ? 'Exige aprovação' : 'Automática'),
    },
  ]

  const colunasPendentes = [
    { chave: 'areaComumNome', titulo: 'Área' },
    {
      chave: 'unidadeNumero', titulo: 'Unidade',
      renderizar: (r) => `${r.blocoNome}, ${r.unidadeNumero}`,
    },
    { chave: 'data', titulo: 'Data', renderizar: (r) => formatarData(r.data) },
    {
      chave: 'horaInicio', titulo: 'Horário',
      renderizar: (r) => `${formatarHora(r.horaInicio)}–${formatarHora(r.horaFim)}`,
    },
    { chave: 'finalidade', titulo: 'Finalidade' },
    { chave: 'criadoPorNome', titulo: 'Solicitado por' },
    {
      chave: 'acoes', titulo: 'Ações', ordenavel: false,
      renderizar: (r) =>
        recusandoId === r.id ? (
          <div className="formulario-correcao">
            <label className="campo">
              <span>Motivo da recusa</span>
              <textarea value={motivoRecusa} onChange={(e) => setMotivoRecusa(e.target.value)} rows={2} />
            </label>
            <div className="tabela-dados-acoes">
              <button type="button" className="botao-primario" onClick={() => confirmarRecusa(r)} disabled={salvandoAcaoId === r.id}>
                {salvandoAcaoId === r.id ? 'Salvando...' : 'Confirmar recusa'}
              </button>
              <button type="button" className="botao-secundario" onClick={cancelarRecusa}>Cancelar</button>
            </div>
          </div>
        ) : (
          <div className="tabela-dados-acoes">
            <button type="button" className="botao-primario" onClick={() => aprovar(r)} disabled={salvandoAcaoId === r.id}>
              {salvandoAcaoId === r.id ? 'Salvando...' : 'Aprovar'}
            </button>
            <button type="button" className="botao-secundario" onClick={() => abrirRecusa(r)}>Recusar</button>
          </div>
        ),
    },
  ]

  return (
    <section>
      <h2>Áreas comuns e reservas</h2>
      <p className="descricao">
        Cadastre as áreas comuns do condomínio e acompanhe os pedidos de reserva.
        Duas reservas do mesmo espaço no mesmo horário não coexistem, e unidades
        inadimplentes não conseguem reservar.
      </p>

      <h3 className="subtitulo-secao">Nova área comum</h3>
      <form onSubmit={cadastrar} className="formulario-inline">
        <label className="campo">
          <span>Nome</span>
          <input name="nome" value={form.nome} onChange={atualizarCampo} placeholder="Ex: Salão de festas" required />
        </label>
        <label className="campo">
          <span>Capacidade</span>
          <input type="number" name="capacidade" min="1" value={form.capacidade} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Abre às</span>
          <input type="time" name="horarioAbertura" value={form.horarioAbertura} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Fecha às</span>
          <input type="time" name="horarioFechamento" value={form.horarioFechamento} onChange={atualizarCampo} required />
        </label>
        <label className="campo opcao-marcavel">
          <input type="checkbox" name="exigeAprovacao" checked={form.exigeAprovacao} onChange={atualizarCampo} />
          <span>Exige aprovação do síndico</span>
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          {salvando ? 'Cadastrando...' : 'Cadastrar área'}
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}

      <h3 className="subtitulo-secao">Áreas cadastradas</h3>
      <TabelaDados
        colunas={colunasAreas}
        dados={areas}
        camposBusca={['nome']}
        buscaPlaceholder="Buscar por nome..."
        mensagemVazia={carregandoAreas ? 'Carregando...' : 'Nenhuma área comum cadastrada ainda.'}
        mensagemErro={erroAreas}
      />

      <h3 className="subtitulo-secao">Reservas pendentes de aprovação</h3>
      {erroAcao && <p className="alerta-erro">{erroAcao.mensagem}</p>}
      <TabelaDados
        colunas={colunasPendentes}
        dados={pendentes}
        camposBusca={['areaComumNome', 'criadoPorNome', 'unidadeNumero']}
        buscaPlaceholder="Buscar por área, unidade ou quem pediu..."
        mensagemVazia={carregandoPendentes ? 'Carregando...' : 'Nenhuma reserva pendente.'}
        mensagemErro={erroPendentes}
      />

      <h3 className="subtitulo-secao">Calendário de reservas</h3>
      <CalendarioReservas condominioId={condominioId} />
    </section>
  )
}

export default AreasComuns
