function CamposDespesa({ form, aoMudarCampo }) {
  return (
    <>
      <div className="linha-dois-campos">
        <label className="campo">
          <span>Data</span>
          <input type="date" name="data" value={form.data} onChange={aoMudarCampo} required />
        </label>

        <label className="campo">
          <span>Valor</span>
          <input
            type="number"
            name="valor"
            value={form.valor}
            onChange={aoMudarCampo}
            min="0.01"
            step="0.01"
            placeholder="0,00"
            required
          />
        </label>
      </div>

      <div className="linha-dois-campos">
        <label className="campo">
          <span>Categoria</span>
          <input
            name="categoria"
            value={form.categoria}
            onChange={aoMudarCampo}
            placeholder="Ex: Água, Luz, Limpeza, Manutenção, Folha, Obra"
            required
          />
        </label>

        <label className="campo">
          <span>Fornecedor</span>
          <input name="fornecedor" value={form.fornecedor} onChange={aoMudarCampo} required />
        </label>
      </div>

      <label className="campo">
        <span>Tipo</span>
        <select name="tipo" value={form.tipo} onChange={aoMudarCampo}>
          <option value="ORDINARIA">Ordinária (paga pelo ocupante)</option>
          <option value="EXTRAORDINARIA">Extraordinária (paga pelo proprietário)</option>
        </select>
      </label>

      {form.tipo === 'EXTRAORDINARIA' && (
        <label className="campo campo-curto">
          <span>Parcelas</span>
          <input
            type="number"
            name="numeroParcelas"
            value={form.numeroParcelas}
            onChange={aoMudarCampo}
            min="1"
            placeholder="Ex: 10"
            required
          />
        </label>
      )}

      <label className="campo">
        <span>Comprovante</span>
        <input
          name="comprovante"
          value={form.comprovante}
          onChange={aoMudarCampo}
          placeholder="Nome do arquivo ou referência da nota fiscal"
          required
        />
      </label>
    </>
  )
}

export default CamposDespesa