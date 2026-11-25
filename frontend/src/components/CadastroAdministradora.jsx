import { useState } from 'react'
import { formatarCnpj } from '../utils/cnpj.js'

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
    const valorTratado = name === 'cnpj' ? formatarCnpj(value) : value
    setForm((formAnterior) => ({
      ...formAnterior,
      [name]: valorTratado,
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
        Insira os dados da empresa administradora do condomínio.
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
            inputMode="numeric"
            maxLength={18}
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
          <p className="confirmacao">Dados salvos.</p>
        )}
      </form>
    </section>
  )
}

export default CadastroAdministradora