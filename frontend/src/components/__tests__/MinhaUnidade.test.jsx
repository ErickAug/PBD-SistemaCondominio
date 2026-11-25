import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import MinhaUnidade from '../MinhaUnidade.jsx'

vi.mock('../ListasDaUnidade.jsx', () => ({
  default: (props) => <div data-testid="listas-da-unidade">{JSON.stringify(props)}</div>,
}))

describe('MinhaUnidade', () => {
  it('mostra aviso quando não há bloco ou unidade identificados', () => {
    render(<MinhaUnidade condominioId={1} blocoId={null} unidadeId={null} usuarioId={9} />)

    expect(
      screen.getByText(
        'Não foi possível identificar a sua unidade. Se você acabou de ser cadastrado, peça ao síndico para registrar a sua entrada.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByTestId('listas-da-unidade')).not.toBeInTheDocument()
  })

  it('mostra aviso quando o usuário não mora na unidade selecionada', () => {
    render(
      <MinhaUnidade
        condominioId={1}
        blocoId={2}
        unidadeId={10}
        blocoNome="Bloco A"
        unidadeNumero="101"
        usuarioId={9}
        podeUsar={false}
      />,
    )

    expect(
      screen.getByText(
        'Você não mora nesta unidade, então não gerencia moradores, dependentes, veículos ou visitas por ela.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByTestId('listas-da-unidade')).not.toBeInTheDocument()
  })

  it('mostra a gestão da unidade quando o usuário mora nela', () => {
    render(
      <MinhaUnidade
        condominioId={1}
        blocoId={2}
        unidadeId={10}
        blocoNome="Bloco A"
        unidadeNumero="101"
        usuarioId={9}
      />,
    )

    expect(screen.getByText('Minha unidade: Bloco A, unidade 101')).toBeInTheDocument()
    const listas = screen.getByTestId('listas-da-unidade')
    const props = JSON.parse(listas.textContent)
    expect(props).toEqual({ condominioId: 1, blocoId: 2, unidadeId: 10, donoFixoId: 9 })
  })
})
