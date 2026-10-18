import { useState, useEffect, useCallback } from 'react'
import {
  listarMoradores,
  registrarMorador,
  encerrarVinculoMorador,
  listarDependentes,
  cadastrarDependente,
  desativarDependente,
  listarAutorizados,
  cadastrarAutorizado,
  listarVagas,
  criarVaga,
  excluirVaga,
  listarVeiculos,
  cadastrarVeiculo,
  desativarVeiculo,
} from '../services/api.js'

const ABAS = [
  { valor: 'moradores', rotulo: 'Moradores' },
  { valor: 'dependentes', rotulo: 'Dependentes' },
  { valor: 'autorizados', rotulo: 'Autorizados permanentes' },
  { valor: 'vagas', rotulo: 'Vagas' },
  { valor: 'veiculos', rotulo: 'Veículos' },
]

const TIPOS_SUGERIDOS = ['Diarista', 'Cuidador', 'Professor particular']

function formatarData(data) {
  return data.split('-').reverse().join('/')
}

function hoje() {
  const d = new Date()
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

function SecaoMoradores({
  condominioId,
  blocoId,
  unidadeId,
  usuarios,
  versao,
  ocupacaoAtual,
  aoSairOcupante,
}) {
  const [moradores, setMoradores] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [moradorId, setMoradorId] = useState('')
  const [dataEntrada, setDataEntrada] = useState(hoje())
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [encerrandoId, setEncerrandoId] = useState(null)
  const [dataSaida, setDataSaida] = useState(hoje())
  const [erroEncerrar, setErroEncerrar] = useState(null)
  const [registrarVazia, setRegistrarVazia] = useState(true)
  const [avisoSaida, setAvisoSaida] = useState('')

  const carregar = useCallback(async () => {
    try {
      setMoradores(await listarMoradores(condominioId, blocoId, unidadeId))
      setErro('')
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId, blocoId, unidadeId])

  useEffect(() => {
    carregar()
  }, [carregar, versao])

  const candidatos = usuarios.filter(
    (u) => u.perfil === 'MORADOR' && !moradores.some((m) => m.moradorId === u.id)
  )

  async function registrar(evento) {
    evento.preventDefault()
    setErroForm('')

    if (!moradorId) {
      setErroForm('Selecione o morador.')
      return
    }

    setSalvando(true)
    try {
      await registrarMorador(condominioId, blocoId, unidadeId, {
        moradorId: Number(moradorId),
        dataEntrada,
      })
      setMoradorId('')
      await carregar()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  function ehInquilinoAtual(vinculo) {
    return ocupacaoAtual?.tipo === 'ALUGADA' && ocupacaoAtual.ocupanteId === vinculo.moradorId
  }

  async function confirmarEncerramento(vinculo) {
    setErroEncerrar(null)
    setAvisoSaida('')

    const tambemVazia = Boolean(aoSairOcupante) && ehInquilinoAtual(vinculo) && registrarVazia

    if (tambemVazia && dataSaida <= ocupacaoAtual.dataEntrada) {
      setErroEncerrar({
        id: vinculo.id,
        mensagem: `Para registrar a unidade como vazia, a data de saída precisa ser posterior à entrada do inquilino (${formatarData(ocupacaoAtual.dataEntrada)}).`,
      })
      return
    }

    try {
      await encerrarVinculoMorador(condominioId, blocoId, unidadeId, vinculo.id, dataSaida)
    } catch (e) {
      setErroEncerrar({ id: vinculo.id, mensagem: e.message })
      return
    }

    if (tambemVazia) {
      try {
        await aoSairOcupante(dataSaida)
      } catch (e) {
        setAvisoSaida(
          `O vínculo foi encerrado, mas a unidade não foi registrada como vazia: ${e.message}`
        )
      }
    }

    setEncerrandoId(null)
    await carregar()
  }

  if (carregando) return <p className="aviso-vazio">Carregando...</p>

  return (
    <div>
      {erro && <p className="alerta-erro">{erro}</p>}

      <h5 className="subtitulo-lista">
        {moradores.length === 0
          ? 'Nenhum morador ativo nesta unidade'
          : `${moradores.length} morador(es) ativo(s)`}
      </h5>

      {avisoSaida && <p className="alerta-erro">{avisoSaida}</p>}

      {ocupacaoAtual?.tipo === 'ALUGADA' &&
        !moradores.some((m) => m.moradorId === ocupacaoAtual.ocupanteId) && (
          <p className="aviso-vazio">
            O inquilino atual ({ocupacaoAtual.ocupanteNome}) não tem vínculo ativo de morador
            nesta unidade.
          </p>
        )}

      {ocupacaoAtual?.tipo === 'VAZIA' && moradores.length > 0 && (
        <p className="aviso-vazio">
          A unidade está registrada como vazia, mas ainda há morador ativo.
        </p>
      )}

      <ul className="lista-condominios">
        {moradores.map((m) => {
          const contato = usuarios.find((u) => u.id === m.moradorId)?.contato
          return (
            <li key={m.id} className="item-unidade">
              <div className="item-condominio">
                <div>
                  <strong>{m.moradorNome}</strong>
                  <p>
                    Desde {formatarData(m.dataEntrada)}
                    {contato ? ` · ${contato}` : ''}
                  </p>
                </div>
                {encerrandoId !== m.id && (
                  <button
                    type="button"
                    className="botao-remover"
                    onClick={() => {
                      setEncerrandoId(m.id)
                      setErroEncerrar(null)
                      setAvisoSaida('')
                      setRegistrarVazia(true)
                      setDataSaida(hoje())
                    }}
                  >
                    Encerrar vínculo
                  </button>
                )}
              </div>

              {encerrandoId === m.id && (
                <form
                  className="formulario-inline linha-encerrar"
                  onSubmit={(e) => {
                    e.preventDefault()
                    confirmarEncerramento(m)
                  }}
                >
                  <p className="descricao-curta nota-linha">
                    Os veículos deste morador na unidade também serão desativados, e as vagas ficam livres.
                  </p>
                  <label className="campo">
                    <span>Data de saída</span>
                    <input
                      type="date"
                      value={dataSaida}
                      onChange={(e) => setDataSaida(e.target.value)}
                      required
                    />
                  </label>
                  {aoSairOcupante && ehInquilinoAtual(m) && (
                    <label className="opcao-marcavel">
                      <input
                        type="checkbox"
                        checked={registrarVazia}
                        onChange={(e) => setRegistrarVazia(e.target.checked)}
                      />
                      <span>Registrar também a unidade como vazia a partir desta data</span>
                    </label>
                  )}
                  <button type="submit" className="botao-primario">
                    Confirmar saída
                  </button>
                  <button
                    type="button"
                    className="botao-secundario"
                    onClick={() => setEncerrandoId(null)}
                  >
                    Cancelar
                  </button>
                </form>
              )}

              {erroEncerrar?.id === m.id && (
                <p className="alerta-erro">{erroEncerrar.mensagem}</p>
              )}
            </li>
          )
        })}
      </ul>

      <h5 className="subtitulo-lista">Adicionar morador</h5>
      {candidatos.length === 0 ? (
        <p className="aviso-vazio">
          Nenhum morador disponível para vincular. O síndico precisa criar o acesso
          (usuário com perfil Morador) antes.
        </p>
      ) : (
        <form onSubmit={registrar} className="formulario-inline">
          <label className="campo">
            <span>Morador</span>
            <select
              value={moradorId}
              onChange={(e) => {
                setMoradorId(e.target.value)
                setErroForm('')
              }}
            >
              <option value="">Selecione</option>
              {candidatos.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.nome}
                </option>
              ))}
            </select>
          </label>
          <label className="campo">
            <span>Data de entrada</span>
            <input
              type="date"
              value={dataEntrada}
              onChange={(e) => setDataEntrada(e.target.value)}
              required
            />
          </label>
          <button type="submit" className="botao-primario" disabled={salvando}>
            Adicionar morador
          </button>
        </form>
      )}
      {erroForm && <p className="alerta-erro">{erroForm}</p>}
    </div>
  )
}

function SecaoDependentes({ condominioId, blocoId, unidadeId }) {
  const [dependentes, setDependentes] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [nome, setNome] = useState('')
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erroItem, setErroItem] = useState(null)

  const carregar = useCallback(async () => {
    try {
      setDependentes(await listarDependentes(condominioId, blocoId, unidadeId))
      setErro('')
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId, blocoId, unidadeId])

  useEffect(() => {
    carregar()
  }, [carregar])

  async function adicionar(evento) {
    evento.preventDefault()
    setErroForm('')
    setSalvando(true)
    try {
      await cadastrarDependente(condominioId, blocoId, unidadeId, nome.trim())
      setNome('')
      await carregar()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function desativar(dependente) {
    setErroItem(null)
    try {
      await desativarDependente(condominioId, blocoId, unidadeId, dependente.id)
      await carregar()
    } catch (e) {
      setErroItem({ id: dependente.id, mensagem: e.message })
    }
  }

  if (carregando) return <p className="aviso-vazio">Carregando...</p>

  return (
    <div>
      {erro && <p className="alerta-erro">{erro}</p>}

      <h5 className="subtitulo-lista">
        {dependentes.length === 0
          ? 'Nenhum dependente cadastrado'
          : `${dependentes.length} dependente(s)`}
      </h5>

      <p className="descricao-curta">
        Dependentes aparecem na unidade, mas não têm acesso ao sistema.
      </p>

      <ul className="lista-condominios">
        {dependentes.map((d) => (
          <li key={d.id} className="item-unidade">
            <div className="item-condominio">
              <strong>{d.nome}</strong>
              <button type="button" className="botao-remover" onClick={() => desativar(d)}>
                Desativar
              </button>
            </div>
            {erroItem?.id === d.id && <p className="alerta-erro">{erroItem.mensagem}</p>}
          </li>
        ))}
      </ul>

      <h5 className="subtitulo-lista">Adicionar dependente</h5>
      <form onSubmit={adicionar} className="formulario-inline">
        <label className="campo">
          <span>Nome</span>
          <input
            value={nome}
            onChange={(e) => {
              setNome(e.target.value)
              setErroForm('')
            }}
            placeholder="Ex: Filho Pequeno"
            required
          />
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          Adicionar dependente
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}
    </div>
  )
}

function SecaoAutorizados({ condominioId, blocoId, unidadeId }) {
  const formVazio = { nome: '', documento: '', tipo: '', dataInicio: hoje(), dataFim: '' }

  const [autorizados, setAutorizados] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)

  const carregar = useCallback(async () => {
    try {
      setAutorizados(await listarAutorizados(condominioId, blocoId, unidadeId))
      setErro('')
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId, blocoId, unidadeId])

  useEffect(() => {
    carregar()
  }, [carregar])

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErroForm('')
  }

  async function adicionar(evento) {
    evento.preventDefault()
    setErroForm('')

    if (form.dataFim <= form.dataInicio) {
      setErroForm('A data final da validade precisa ser posterior à data inicial.')
      return
    }

    setSalvando(true)
    try {
      await cadastrarAutorizado(condominioId, blocoId, unidadeId, {
        nome: form.nome.trim(),
        documento: form.documento.trim(),
        tipo: form.tipo.trim(),
        dataInicio: form.dataInicio,
        dataFim: form.dataFim,
      })
      setForm(formVazio)
      await carregar()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  if (carregando) return <p className="aviso-vazio">Carregando...</p>

  return (
    <div>
      {erro && <p className="alerta-erro">{erro}</p>}

      <h5 className="subtitulo-lista">
        {autorizados.length === 0
          ? 'Nenhum autorizado permanente cadastrado'
          : `${autorizados.length} autorizado(s) permanente(s)`}
      </h5>

      <p className="descricao-curta">
        Entram sem precisar de autorização a cada visita, enquanto a validade estiver em dia.
      </p>

      <ul className="lista-condominios">
        {autorizados.map((a) => (
          <li key={a.id} className="item-unidade">
            <div className="item-condominio">
              <div>
                <strong>{a.nome}</strong>
                <p>
                  {a.tipo} · Doc. {a.documento}
                </p>
                <p>
                  Validade: {formatarData(a.dataInicio)} a {formatarData(a.dataFim)}
                </p>
              </div>
              <span className={a.liberadoHoje ? 'selo selo-ativo' : 'selo selo-vencido'}>
                {a.liberadoHoje ? 'Liberado hoje' : 'Fora da validade'}
              </span>
            </div>
          </li>
        ))}
      </ul>

      <h5 className="subtitulo-lista">Adicionar autorizado</h5>
      <form onSubmit={adicionar} className="formulario-inline">
        <label className="campo">
          <span>Nome</span>
          <input
            name="nome"
            value={form.nome}
            onChange={atualizarCampo}
            placeholder="Ex: Diarista Joana"
            required
          />
        </label>
        <label className="campo">
          <span>Documento</span>
          <input name="documento" value={form.documento} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Tipo</span>
          <input
            name="tipo"
            list="tipos-autorizado"
            value={form.tipo}
            onChange={atualizarCampo}
            placeholder="Ex: Diarista"
            required
          />
          <datalist id="tipos-autorizado">
            {TIPOS_SUGERIDOS.map((t) => (
              <option key={t} value={t} />
            ))}
          </datalist>
        </label>
        <label className="campo">
          <span>Válido de</span>
          <input
            type="date"
            name="dataInicio"
            value={form.dataInicio}
            onChange={atualizarCampo}
            required
          />
        </label>
        <label className="campo">
          <span>Válido até</span>
          <input
            type="date"
            name="dataFim"
            value={form.dataFim}
            onChange={atualizarCampo}
            required
          />
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          Adicionar autorizado
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}
    </div>
  )
}

const TIPOS_VAGA = [
  { valor: 'COBERTA', rotulo: 'Coberta' },
  { valor: 'DESCOBERTA', rotulo: 'Descoberta' },
]

function rotuloDoTipoVaga(tipo) {
  return TIPOS_VAGA.find((t) => t.valor === tipo)?.rotulo ?? tipo
}

function SecaoVagas({ condominioId, blocoId, unidadeId, podeGerenciar, versao }) {
  const [dados, setDados] = useState({ quantidade: 0, vagas: [] })
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [numero, setNumero] = useState('')
  const [tipo, setTipo] = useState('COBERTA')
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erroItem, setErroItem] = useState(null)

  const carregar = useCallback(async () => {
    try {
      setDados(await listarVagas(condominioId, blocoId, unidadeId))
      setErro('')
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId, blocoId, unidadeId])

  useEffect(() => {
    carregar()
  }, [carregar, versao])

  async function adicionar(evento) {
    evento.preventDefault()
    setErroForm('')
    setSalvando(true)
    try {
      await criarVaga(condominioId, blocoId, unidadeId, { numero: numero.trim(), tipo })
      setNumero('')
      await carregar()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function excluir(vaga) {
    setErroItem(null)
    try {
      await excluirVaga(condominioId, blocoId, unidadeId, vaga.id)
      await carregar()
    } catch (e) {
      setErroItem({ id: vaga.id, mensagem: e.message })
    }
  }

  if (carregando) return <p className="aviso-vazio">Carregando...</p>

  return (
    <div>
      {erro && <p className="alerta-erro">{erro}</p>}

      <h5 className="subtitulo-lista">
        {dados.quantidade === 0
          ? 'Nenhuma vaga cadastrada para esta unidade'
          : `${dados.quantidade} vaga(s) nesta unidade`}
      </h5>

      <ul className="lista-condominios">
        {dados.vagas.map((v) => (
          <li key={v.id} className="item-unidade">
            <div className="item-condominio">
              <div>
                <strong>Vaga {v.numero}</strong>
                <p>
                  {rotuloDoTipoVaga(v.tipo)}
                  {v.veiculo
                    ? ` · ${v.veiculo.placa} · ${v.veiculo.modelo} ${v.veiculo.cor}`
                    : ''}
                </p>
              </div>
              <div className="item-condominio-acoes">
                <span className={v.veiculo ? 'selo selo-inativo' : 'selo selo-ativo'}>
                  {v.veiculo ? 'Ocupada' : 'Livre'}
                </span>
                {podeGerenciar && (
                  <button type="button" className="botao-remover" onClick={() => excluir(v)}>
                    Excluir
                  </button>
                )}
              </div>
            </div>
            {erroItem?.id === v.id && <p className="alerta-erro">{erroItem.mensagem}</p>}
          </li>
        ))}
      </ul>

      {podeGerenciar && (
        <>
          <h5 className="subtitulo-lista">Adicionar vaga</h5>
          <p className="descricao-curta">O número da vaga é único no condomínio inteiro.</p>
          <form onSubmit={adicionar} className="formulario-inline">
            <label className="campo">
              <span>Número</span>
              <input
                value={numero}
                onChange={(e) => {
                  setNumero(e.target.value)
                  setErroForm('')
                }}
                placeholder="Ex: 12"
                required
              />
            </label>
            <label className="campo">
              <span>Tipo</span>
              <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
                {TIPOS_VAGA.map((t) => (
                  <option key={t.valor} value={t.valor}>
                    {t.rotulo}
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" className="botao-primario" disabled={salvando}>
              Adicionar vaga
            </button>
          </form>
          {erroForm && <p className="alerta-erro">{erroForm}</p>}
        </>
      )}
    </div>
  )
}

function SecaoVeiculos({ condominioId, blocoId, unidadeId, donoFixoId, ocupacaoAtual, versao }) {
  const formVazio = { placa: '', modelo: '', cor: '', donoId: '', vagaId: '' }

  const [veiculos, setVeiculos] = useState([])
  const [vagasLivres, setVagasLivres] = useState([])
  const [moradores, setMoradores] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [form, setForm] = useState(formVazio)
  const [erroForm, setErroForm] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erroItem, setErroItem] = useState(null)

  const carregar = useCallback(async () => {
    try {
      const [listaVeiculos, dadosVagas, listaMoradores] = await Promise.all([
        listarVeiculos(condominioId, blocoId, unidadeId),
        listarVagas(condominioId, blocoId, unidadeId),
        donoFixoId ? Promise.resolve([]) : listarMoradores(condominioId, blocoId, unidadeId),
      ])
      setVeiculos(listaVeiculos)
      setVagasLivres(dadosVagas.vagas.filter((v) => !v.veiculo))
      setMoradores(listaMoradores)
      setErro('')
    } catch (e) {
      setErro(e.message)
    } finally {
      setCarregando(false)
    }
  }, [condominioId, blocoId, unidadeId, donoFixoId])

  useEffect(() => {
    carregar()
  }, [carregar, versao])

  const opcoesDono = moradores.map((m) => ({ id: m.moradorId, nome: m.moradorNome }))
  if (
    ocupacaoAtual?.tipo === 'PROPRIETARIO_MORANDO' &&
    ocupacaoAtual.ocupanteId &&
    !opcoesDono.some((o) => o.id === ocupacaoAtual.ocupanteId)
  ) {
    opcoesDono.push({
      id: ocupacaoAtual.ocupanteId,
      nome: `${ocupacaoAtual.ocupanteNome} (proprietário)`,
    })
  }

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: name === 'placa' ? value.toUpperCase() : value }))
    setErroForm('')
  }

  async function cadastrar(evento) {
    evento.preventDefault()
    setErroForm('')

    if (!donoFixoId && !form.donoId) {
      setErroForm('Selecione o dono do veículo.')
      return
    }

    setSalvando(true)
    try {
      await cadastrarVeiculo(condominioId, blocoId, unidadeId, {
        donoId: donoFixoId ?? Number(form.donoId),
        placa: form.placa.trim(),
        modelo: form.modelo.trim(),
        cor: form.cor.trim(),
        vagaId: form.vagaId ? Number(form.vagaId) : null,
      })
      setForm(formVazio)
      await carregar()
    } catch (e) {
      setErroForm(e.message)
    } finally {
      setSalvando(false)
    }
  }

  async function desativar(veiculo) {
    setErroItem(null)
    try {
      await desativarVeiculo(condominioId, blocoId, unidadeId, veiculo.id)
      await carregar()
    } catch (e) {
      setErroItem({ id: veiculo.id, mensagem: e.message })
    }
  }

  if (carregando) return <p className="aviso-vazio">Carregando...</p>

  return (
    <div>
      {erro && <p className="alerta-erro">{erro}</p>}

      <h5 className="subtitulo-lista">
        {veiculos.length === 0
          ? 'Nenhum veículo ativo nesta unidade'
          : `${veiculos.length} veículo(s) ativo(s)`}
      </h5>

      <ul className="lista-condominios">
        {veiculos.map((v) => (
          <li key={v.id} className="item-unidade">
            <div className="item-condominio">
              <div>
                <strong>{v.placa}</strong>
                <p>
                  {v.modelo} · {v.cor}
                </p>
                <p>
                  Dono: {v.donoNome} · {v.vagaNumero ? `Vaga ${v.vagaNumero}` : 'Sem vaga'}
                </p>
              </div>
              <button type="button" className="botao-remover" onClick={() => desativar(v)}>
                Desativar
              </button>
            </div>
            {erroItem?.id === v.id && <p className="alerta-erro">{erroItem.mensagem}</p>}
          </li>
        ))}
      </ul>

      <h5 className="subtitulo-lista">Cadastrar veículo</h5>
      <p className="descricao-curta">
        A placa é única no condomínio. A vaga é opcional, e só as vagas livres aparecem na lista.
      </p>
      <form onSubmit={cadastrar} className="formulario-inline">
        <label className="campo">
          <span>Placa</span>
          <input
            name="placa"
            value={form.placa}
            onChange={atualizarCampo}
            placeholder="ABC-1234"
            required
          />
        </label>
        <label className="campo">
          <span>Modelo</span>
          <input name="modelo" value={form.modelo} onChange={atualizarCampo} required />
        </label>
        <label className="campo">
          <span>Cor</span>
          <input name="cor" value={form.cor} onChange={atualizarCampo} required />
        </label>
        {!donoFixoId && (
          <label className="campo">
            <span>Dono</span>
            <select name="donoId" value={form.donoId} onChange={atualizarCampo}>
              <option value="">Selecione</option>
              {opcoesDono.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.nome}
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="campo">
          <span>Vaga</span>
          <select name="vagaId" value={form.vagaId} onChange={atualizarCampo}>
            <option value="">Sem vaga</option>
            {vagasLivres.map((v) => (
              <option key={v.id} value={v.id}>
                Vaga {v.numero} ({rotuloDoTipoVaga(v.tipo)})
              </option>
            ))}
          </select>
        </label>
        <button type="submit" className="botao-primario" disabled={salvando}>
          Cadastrar veículo
        </button>
      </form>
      {erroForm && <p className="alerta-erro">{erroForm}</p>}
    </div>
  )
}

function ListasDaUnidade({
  condominioId,
  blocoId,
  unidadeId,
  usuarios = [],
  versao = 0,
  ocupacaoAtual = null,
  aoSairOcupante = null,
  donoFixoId = null,
  podeGerenciarVagas = false,
}) {
  const [aba, setAba] = useState('moradores')
  const ids = { condominioId, blocoId, unidadeId }

  return (
    <div className="listas-unidade">
      <div className="abas">
        {ABAS.map((a) => (
          <button
            key={a.valor}
            type="button"
            className={aba === a.valor ? 'aba ativa' : 'aba'}
            onClick={() => setAba(a.valor)}
          >
            {a.rotulo}
          </button>
        ))}
      </div>

      {aba === 'moradores' && (
        <SecaoMoradores
          {...ids}
          usuarios={usuarios}
          versao={versao}
          ocupacaoAtual={ocupacaoAtual}
          aoSairOcupante={aoSairOcupante}
        />
      )}
      {aba === 'dependentes' && <SecaoDependentes {...ids} />}
      {aba === 'autorizados' && <SecaoAutorizados {...ids} />}
      {aba === 'vagas' && (
        <SecaoVagas {...ids} podeGerenciar={podeGerenciarVagas} versao={versao} />
      )}
      {aba === 'veiculos' && (
        <SecaoVeiculos
          {...ids}
          donoFixoId={donoFixoId}
          ocupacaoAtual={ocupacaoAtual}
          versao={versao}
        />
      )}
    </div>
  )
}

export default ListasDaUnidade