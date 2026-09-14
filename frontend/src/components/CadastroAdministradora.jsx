import { useState } from 'react'

function CadastroAdministradora() {
  const [form, setForm] = useState({
    nomeFantasia: '',
    razaoSocial: '',
    cnpj: '',
    emailContato: '',
  })
  const [salvo, setSalvo] = useState(false)

  function atualizarCampo(evento) {
    const { name, value } = evento.target
    setForm((formAnterior) => ({
      ...formAnterior,
      [name]: value,
    }))
  }

  function aoEnviar(evento) {
    evento.preventDefault()
    console.log('Dados da administradora:', form)
    setSalvo(true)
  }

  return (
    <section>
      <h2>Dados da administradora</h2>
      <p className="descricao">
        Essas são as informações da própria empresa administradora,
        exibidas no relatório e usadas para identificar quem
        gerencia cada condomínio no sistema.
      </p>

      <form onSubmit={aoEnviar} className="formulario">
        <label className="campo">
          <span>Nome fantasia</span>
          <input
            name="nomeFantasia"
            value={form.nomeFantasia}
            onChange={atualizarCampo}
            placeholder="Ex: Condovix Administração"
            required
          />
        </label>

        <label className="campo">
          <span>Razão social</span>
          <input
            name="razaoSocial"
            value={form.razaoSocial}
            onChange={atualizarCampo}
            placeholder="Ex: Condovix Administração de Bens Ltda"
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

        <label className="campo">
          <span>E-mail de contato</span>
          <input
            type="email"
            name="emailContato"
            value={form.emailContato}
            onChange={atualizarCampo}
            placeholder="contato@administradora.com"
            required
          />
        </label>

        <button type="submit" className="botao-primario">
          Salvar dados
        </button>

        {salvo && (
          <p className="confirmacao">Dados salvos localmente (ainda sem backend).</p>
        )}
      </form>
    </section>
  )
}

export default CadastroAdministradora