import ListasDaUnidade from './ListasDaUnidade.jsx'

function MinhaUnidade({ usuarioLogado }) {
  const { condominioId, blocoId, unidadeId } = usuarioLogado

  return (
    <section>
      <h2>Minha unidade</h2>
      <p className="descricao">
        Aqui você cadastra quem mora com você, os dependentes e quem pode
        entrar sempre, sem precisar autorizar a cada visita.
      </p>

      {!blocoId || !unidadeId ? (
        <p className="aviso-vazio">
          Não foi possível identificar a sua unidade. Se você acabou de ser
          cadastrado, peça ao síndico para registrar a sua entrada.
        </p>
      ) : (
        <ListasDaUnidade
          condominioId={condominioId}
          blocoId={blocoId}
          unidadeId={unidadeId}
        />
      )}
    </section>
  )
}

export default MinhaUnidade