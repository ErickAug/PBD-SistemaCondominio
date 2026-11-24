import { useState } from 'react'
import { criarCondominio } from '../services/api.js'
import TabelaDados from './TabelaDados.jsx'

const COLUNAS_CONDOMINIOS = [
  { chave: 'nome', titulo: 'Nome' },
  { chave: 'endereco', titulo: 'Endereço' },
  { chave: 'cnpj', titulo: 'CNPJ' },
  {
    chave: 'situacao',
    titulo: 'Situação',
    renderizar: (item) => (
      <span className={item.situacao === 'ATIVO' ? 'selo selo-ativo' : 'selo selo-inativo'}>
        {item.situacao === 'ATIVO' ? 'Ativo' : 'Inativo'}
      </span>
    ),
  },
]

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
        Insira os dados do condomínio que deseja cadastrar.
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

      <TabelaDados
        colunas={COLUNAS_CONDOMINIOS}
        dados={condominios}
        camposBusca={['nome', 'endereco', 'cnpj']}
        buscaPlaceholder="Buscar por nome, endereço ou CNPJ..."
        mensagemVazia="Nenhum condomínio cadastrado ainda."
      />
    </section>
  )
}

export default CadastroCondominios