function SeletorUnidade({ unidades, unidadeAtivaId, aoTrocar }) {
  return (
    <label className="seletor-condominio">
      <span>Unidade ativa</span>
      <select
        value={unidadeAtivaId}
        onChange={(evento) => aoTrocar(Number(evento.target.value))}
      >
        {unidades.map((unidade) => (
          <option key={unidade.unidadeId} value={unidade.unidadeId}>
            {unidade.blocoNome}, {unidade.unidadeNumero}
          </option>
        ))}
      </select>
    </label>
  )
}

export default SeletorUnidade
