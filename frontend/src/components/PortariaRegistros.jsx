import { useEffect, useState, useCallback } from 'react'
import {
  listarBlocos,
  listarUnidades,
  registrarEntradaManual,
  registrarSaidaAcesso,
  listarQuemEstaDentro,
} from '../services/api.js'

const TIPOS_REGISTRO = ['Visita', 'Prestador de serviço', 'Entrega', 'Morador', 'Outro']

function formatarHora(dataHora) {
  if (!dataHora) return ''
  return new Date(dataHora).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function horasDesde(dataHora) {
  return (Date.now() - new Date(dataHora).getTime()) / 3600000
}

function PortariaRegistros({ condominioId }) {
  const formVazio = {
    blocoId: '',
    unidadeId: '',
    nome: '',
    documento: '',
    tipo: '',
    quemAutorizou: '',
    placa: '',
  }

  const [blocos, setBlocos] = useState([])
  const [unidades, setUnidades] = useState([])
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [quemEstaDentro, setQuemEstaDentro] = useState([])
  const [carregandoDentro, setCarregandoDentro] = useState(true)
  const [erroDentro, setErroDentro] = useState('')
  const [erroItem, setErroItem] = useState(null)
  const [registrandoSaidaId, setRegistrandoSaidaId] = useState(null)

  useEffect(() => {
    listarBlocos(condominioId)
      .then(setBlocos)
      .catch(() => setBlocos([]))
  }, [condominioId])

  useEffect(() => {
    if (!form.blocoId) {
      setUnidades([])
      return
    }
    listarUnidades(condominioId, form.blocoId)
      .then(setUnidades)
      .catch(() => setUnidades([]))
  }, [condominioId, form.blocoId])

  const carregarQuemEstaDentro = useCallback(async () => {
    setCarregandoDentro(true)
    setErroDentro('')
    try {
      setQuemEstaDentro(await listarQuemEstaDentro(condominioId))
    } catch (e) {
      setErroDentro(e.message)
    } finally {
      setCarregandoDentro(false)
    }
  }, [condominioId])

  useEffect(() => {
    carregarQuemEstaDentro()
  }, [carregarQuemEstaDentro])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => {
      if (name === 'blocoId') {
        return { ...anterior, blocoId: value, unidadeId: '' }
      }
      return { ...anterior, [name]: value }
    })
    setErroForm('')
  }

  async function registrarEntrada(evento) {
    evento.preventDefault()
    setErroForm('')

    if (!form.unidadeId) {
      setErroForm('Selecione a unidade.')
      return
    }
    if (!form.quemAutorizou.trim()) {
      setErroForm('Sem autorização prévia, registre quem, na unidade, liberou a entrada.')
      return
    }

    setSalvando(true)
    try {
      await registrarEntradaManual(condominioId, {
        unidadeId: Number(form.unidadeId),
        nome: form.nome.trim(),
        documento: form.documento.trim(),
        tipo: form.tipo.trim(),
        quemAutorizou: form.quemAutorizou.trim(),
        placa: form.placa.trim() || null,
      })
      setForm(formVazio)
      await carregarQuemEstaDentro()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function registrarSaida(registro) {
    setErroItem(null)
    setRegistrandoSaidaId(registro.id)
    try {
      await registrarSaidaAcesso(condominioId, registro.id)
      await carregarQuemEstaDentro()
    } catch (e) {
      setErroItem({ id: registro.id, mensagem: e.message })
    } finally {
      setRegistrandoSaidaId(null)
    }
  }

  return (
    <section>
      <h2>Registro de entrada</h2>
      <p className="descricao">
        Registre manualmente a entrada de quem não possui autorização prévia cadastrada.
      </p>

      <form onSubmit={registrarEntrada} className="formulario-inline">
        <label className="campo">
          <span>Bloco</span>
          <select name="blocoId" value={form.blocoId} onChange={atualizarCampo} required>
            <option value="">Selecione</option>
            {blocos.map((b) => (
              <option key={b.id} value={b.id}>
                {b.nome}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Unidade</span>
          <select
            name="unidadeId"
            value={form.unidadeId}
            onChange={atualizarCampo}
            disabled={!form.blocoId}
            required
          >
            <option value="">Selecione</option>
            {unidades.map((u) => (
              <option key={u.id} value={u.id}>
                {u.numero}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span>Nome</span>
          <input name="nome" value={form.nome} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Documento</span>
          <input name="documento" value={form.documento} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Tipo</span>
          <input
            name="tipo"
            list="tipos-registro"
            value={form.tipo}
            onChange={atualizarCampo}
            placeholder="Ex: Visita"
            required
          />
          <datalist id="tipos-registro">
            {TIPOS_REGISTRO.map((t) => (
              <option key={t} value={t} />
            ))}
          </datalist>
        </label>
        <label className="campo">
          <span>Placa (opcional)</span>
          <input name="placa" value={form.placa} onChange={atualizarCampo} />
        </label>
        <label className="campo">
          <span>Quem autorizou</span>
          <input
            name="quemAutorizou"
            value={form.quemAutorizou}
            onChange={atualizarCampo}
            placeholder="Nome de quem, na unidade, liberou a entrada"
          />
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          {salvando ? 'Registrando...' : 'Registrar entrada'}
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}

      <h3 className="subtitulo-secao">Quem está dentro agora</h3>
      {erroDentro && <p className="alerta-erro">{erroDentro}</p>}
      {carregandoDentro ? (
        <p className="aviso-vazio">Carregando...</p>
      ) : quemEstaDentro.length === 0 ? (
        <p className="aviso-vazio">Ninguém registrado como dentro no momento.</p>
      ) : (
        <ul className="lista-condominios">
          {quemEstaDentro.map((r) => (
            <li key={r.id} className="item-unidade">
              <div className="item-condominio">
                <div>
                  <strong>{r.nome}</strong>
                  <p>
                    {r.tipo} · Doc. {r.documento}
                  </p>
                  <p>
                    {r.blocoNome}, unidade {r.unidadeNumero}
                  </p>
                  <p>Entrada: {formatarHora(r.horaEntrada)}</p>
                </div>
                {horasDesde(r.horaEntrada) >= 12 && <span className="selo selo-vencido">Demorado</span>}
                <button
                  type="button"
                  className="botao-remover"
                  onClick={() => registrarSaida(r)}
                  disabled={registrandoSaidaId === r.id}
                >
                  {registrandoSaidaId === r.id ? 'Registrando...' : 'Registrar saída'}
                </button>
              </div>
              {erroItem?.id === r.id && <p className="alerta-erro">{erroItem.mensagem}</p>}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

export default PortariaRegistros
