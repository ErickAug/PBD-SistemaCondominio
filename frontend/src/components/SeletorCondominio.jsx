function SeletorCondominio({ condominios, condominioAtivoId, aoTrocar }) {
  return (
    <label className="seletor-condominio">
      <span>Condomínio ativo</span>
      <select
        value={condominioAtivoId}
        onChange={(evento) => aoTrocar(Number(evento.target.value))}
      >
        {condominios.map((condominio) => (
          <option key={condominio.id} value={condominio.id}>
            {condominio.nome}
          </option>
        ))}
      </select>
    </label>
  )
}

export default SeletorCondominio