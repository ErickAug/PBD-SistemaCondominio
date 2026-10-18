import { useState } from 'react'
import { buscarVeiculoPorPlaca } from '../services/api.js'

function BuscaPlaca({ condominioId }) {
  const [placa, setPlaca] = useState('')
  const [placaBuscada, setPlacaBuscada] = useState('')
  const [resultado, setResultado] = useState(undefined)
  const [erro, setErro] = useState('')
  const [buscando, setBuscando] = useState(false)

  async function buscar(evento) {
    evento.preventDefault()
    setErro('')
    setBuscando(true)
    try {
      const encontrado = await buscarVeiculoPorPlaca(condominioId, placa.trim())
      setPlacaBuscada(placa.trim())
      setResultado(encontrado)
    } catch (e) {
      setResultado(undefined)
      setErro(e.message)
    } finally {
      setBuscando(false)
    }
  }

  return (
    <section className="busca-placa">
      <h2>Buscar veículo por placa</h2>
      <p className="descricao">
        Digite a placa para saber de quem é o veículo e a qual unidade avisar.
      </p>

      <form onSubmit={buscar} className="formulario-inline">
        <label className="campo">
          <span>Placa</span>
          <input
            value={placa}
            onChange={(e) => {
              setPlaca(e.target.value.toUpperCase())
              setErro('')
            }}
            placeholder="ABC-1234"
            required
            autoFocus
          />
        </label>
        <button type="submit" className="botao-primario" disabled={buscando}>
          {buscando ? 'Buscando...' : 'Buscar'}
        </button>
      </form>

      {erro && <p className="alerta-erro">{erro}</p>}

      {resultado === null && (
        <p className="aviso-vazio resultado-vazio">
          Placa {placaBuscada} não cadastrada neste condomínio.
        </p>
      )}

      {resultado && (
        <div className="resultado-placa">
          <h3>{resultado.placa}</h3>
          <p className="resultado-placa-veiculo">
            {resultado.modelo} · {resultado.cor}
          </p>
          <div className="papeis">
            <div className="papel">
              <span className="papel-rotulo">Unidade</span>
              <strong>
                {resultado.bloco}, unidade {resultado.unidadeNumero}
              </strong>
            </div>
            <div className="papel">
              <span className="papel-rotulo">Responsável</span>
              <strong>{resultado.donoNome}</strong>
            </div>
            <div className="papel">
              <span className="papel-rotulo">Contato</span>
              <strong>{resultado.donoContato ?? 'Contato não informado'}</strong>
            </div>
            <div className="papel">
              <span className="papel-rotulo">Vaga</span>
              <strong>{resultado.vagaNumero ? `Vaga ${resultado.vagaNumero}` : 'Sem vaga'}</strong>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}

export default BuscaPlaca