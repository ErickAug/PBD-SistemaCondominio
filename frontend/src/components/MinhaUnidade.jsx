import ListasDaUnidade from './ListasDaUnidade.jsx'

function MinhaUnidade({ usuarioLogado }) {
  const { condominioId, blocoId, blocoNome, unidadeId, unidadeNumero, usuarioId } = usuarioLogado

  const titulo =
    blocoNome && unidadeNumero
      ? `Minha unidade: ${blocoNome}, unidade ${unidadeNumero}`
      : 'Minha unidade'

  return (
    <section>
      <h2>{titulo}</h2>
      <p className="descricao">
        Aqui você cadastra quem mora com você, os dependentes, quem pode
        entrar sempre e os seus veículos, sem precisar autorizar a cada visita.
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
          donoFixoId={usuarioId}
        />
      )}
    </section>
  )
}

export default MinhaUnidade