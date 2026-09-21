import { useState } from 'react'
import { criarCondominio } from '../services/api.js'

function CadastroCondominios({ administradoraId, condominios, setCondominios, erroCarregamento }) {
  const [form, setForm] = useState({
    nome: '',
    endereco: '',
    cidade: '',
    uf: '',
    cnpj: '',
    situacao: 'ATIVO',
  })
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
    setErro('')
  }

  async function adicionarCondominio(evento) {
    evento.preventDefault()
    setEnviando(true)
    setErro('')

    try {
      const enderecoCompleto = `${form.endereco}, ${form.cidade}/${form.uf}`
      const novoCondominio = await criarCondominio(administradoraId, {
        nome: form.nome,
        endereco: enderecoCompleto,
        cnpj: form.cnpj,
        situacao: form.situacao,
      })
      setCondominios((listaAnterior) => [...listaAnterior, novoCondominio])
      setForm({ nome: '', endereco: '', cidade: '', uf: '', cnpj: '', situacao: 'ATIVO' })
    } catch (erroRequisicao) {
      setErro(erroRequisicao.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <section>
      <h2>Condomínios administrados</h2>
      <p className="descricao">
        Cada condomínio cadastrado aqui é isolado dos demais: síndicos,
        porteiros e moradores de um condomínio nunca devem enxergar
        dados de outro.
      </p>

      {erroCarregamento && <p className="erro-login">{erroCarregamento}</p>}

      <form onSubmit={adicionarCondominio} className="formulario">
        <label className="campo">
          <span>Nome do condomínio</span>
          <input
            name="nome"
            value={form.nome}
            onChange={atualizarCampo}
            placeholder="Ex: Residencial Jardim das Flores"
            required
          />
        </label>

        <label className="campo">
          <span>Endereço</span>
          <input
            name="endereco"
            value={form.endereco}
            onChange={atualizarCampo}
            placeholder="Rua, número, bairro"
            required
          />
        </label>

        <label className="campo">
          <span>CNPJ</span>
          <input
            name="cnpj"
            value={form.cnpj}
            onChange={atualizarCampo}
            placeholder="00.000.000/0000-00"
            required
          />
        </label>

        <div className="linha-dois-campos">
          <label className="campo">
            <span>Cidade</span>
            <input
              name="cidade"
              value={form.cidade}
              onChange={atualizarCampo}
              required
            />
          </label>

          <label className="campo campo-curto">
            <span>UF</span>
            <input
              name="uf"
              value={form.uf}
              onChange={atualizarCampo}
              maxLength={2}
              placeholder="PE"
              required
            />
          </label>
        </div>

        <label className="campo">
          <span>Situação</span>
          <select name="situacao" value={form.situacao} onChange={atualizarCampo}>
            <option value="ATIVO">Ativo</option>
            <option value="INATIVO">Inativo</option>
          </select>
        </label>

        {erro && <p className="erro-login">{erro}</p>}

        <button type="submit" className="botao-primario" disabled={enviando}>
          {enviando ? 'Salvando...' : 'Adicionar condomínio'}
        </button>
      </form>

      <h3 className="lista-titulo">
        {condominios.length === 0
          ? 'Nenhum condomínio cadastrado ainda'
          : `${condominios.length} condomínio(s) cadastrado(s)`}
      </h3>

      <ul className="lista-condominios">
        {condominios.map((condominio) => (
          <li key={condominio.id} className="item-condominio">
            <div>
              <strong>{condominio.nome}</strong>
              <p>{condominio.endereco}</p>
              <p>CNPJ: {condominio.cnpj}</p>
            </div>
            <span
              className={
                condominio.situacao === 'ATIVO' ? 'selo selo-ativo' : 'selo selo-inativo'
              }
            >
              {condominio.situacao === 'ATIVO' ? 'Ativo' : 'Inativo'}
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default CadastroCondominios