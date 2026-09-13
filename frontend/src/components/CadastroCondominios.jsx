import { useState } from 'react'


function CadastroCondominios({ condominios, setCondominios }) {
  const [form, setForm] = useState({
    nome: '',
    endereco: '',
    cidade: '',
    uf: '',
    cnpj: '',
    situacao: 'ativo',
  })

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((anterior) => ({ ...anterior, [name]: value }))
  }

  function adicionarCondominio(evento) {
    evento.preventDefault()

    const novoCondominio = {
      id: crypto.randomUUID(),
      ...form,
    }

    setCondominios((listaAnterior) => [...listaAnterior, novoCondominio])
    setForm({ nome: '', endereco: '', cidade: '', uf: '', cnpj: '', situacao: 'ativo' })
  }

  function removerCondominio(id) {
    setCondominios((listaAnterior) =>
      listaAnterior.filter((condominio) => condominio.id !== id)
    )
  }

  return (
    <section>
      <h2>Condomínios administrados</h2>
      <p className="descricao">
        Cada condomínio cadastrado aqui é isolado dos demais: síndicos,
        porteiros e moradores de um condomínio nunca devem enxergar
        dados de outro.
      </p>

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
            <option value="ativo">Ativo</option>
            <option value="inativo">Inativo</option>
          </select>
        </label>

        <button type="submit" className="botao-primario">
          Adicionar condomínio
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
              <p>
                {condominio.endereco} — {condominio.cidade}/{condominio.uf}
              </p>
              <p>CNPJ: {condominio.cnpj}</p>
            </div>
            <div className="item-condominio-acoes">
              <span
                className={
                  condominio.situacao === 'ativo'
                    ? 'selo selo-ativo'
                    : 'selo selo-inativo'
                }
              >
                {condominio.situacao === 'ativo' ? 'Ativo' : 'Inativo'}
              </span>
              <button
                onClick={() => removerCondominio(condominio.id)}
                className="botao-remover"
                type="button"
              >
                Remover
              </button>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default CadastroCondominios