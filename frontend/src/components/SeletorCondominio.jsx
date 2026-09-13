function SeletorCondominio({ condominios, condominioAtivo, aoTrocar }) {
  return (
    <label className="seletor-condominio">
      <span>Condomínio ativo</span>
      <select
        value={condominioAtivo}
        onChange={(evento) => aoTrocar(evento.target.value)}
      >
        {condominios.map((nome) => (
          <option key={nome} value={nome}>
            {nome}
          </option>
        ))}
      </select>
    </label>
  )
}

export default SeletorCondominio