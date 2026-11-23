import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PrestacaoContasUnidade from '../PrestacaoContasUnidade.jsx'
import { buscarRelatorioPeriodo } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  buscarRelatorioPeriodo: vi.fn(),
}))

const relatorioComDados = {
  periodo: {
    competencia: '2026-11-01',
    receita: 15000,
    totalDespesa: 9000,
    saldo: 6000,
    despesasPorCategoria: [{ categoria: 'Limpeza', valor: 4000 }],
  },
  mesAnterior: {
    competencia: '2026-10-01',
    receita: 14000,
    totalDespesa: 8500,
    saldo: 5500,
    despesasPorCategoria: [],
  },
  inadimplenciaAcumulada: 2300,
}

const relatorioZerado = {
  periodo: {
    competencia: '2026-11-01',
    receita: 0,
    totalDespesa: 0,
    saldo: 0,
    despesasPorCategoria: [],
  },
  mesAnterior: {
    competencia: '2026-10-01',
    receita: 0,
    totalDespesa: 0,
    saldo: 0,
    despesasPorCategoria: [],
  },
  inadimplenciaAcumulada: 0,
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('PrestacaoContasUnidade', () => {
  it('mostra o estado de carregamento antes da resposta chegar', () => {
    buscarRelatorioPeriodo.mockReturnValue(new Promise(() => {}))
    render(<PrestacaoContasUnidade condominioId={1} />)
    expect(screen.getByText('Carregando...')).toBeInTheDocument()
  })

  it('busca o relatório da competência atual ao montar', () => {
    buscarRelatorioPeriodo.mockReturnValue(new Promise(() => {}))
    render(<PrestacaoContasUnidade condominioId={1} />)
    expect(buscarRelatorioPeriodo).toHaveBeenCalledTimes(1)
    const [condominioId, competencia] = buscarRelatorioPeriodo.mock.calls[0]
    expect(condominioId).toBe(1)
    expect(competencia).toMatch(/^\d{4}-\d{2}-01$/)
  })

  it('exibe os valores do relatório após carregar', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioComDados)
    render(<PrestacaoContasUnidade condominioId={1} />)

    expect(await screen.findByText('R$ 15.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 9.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 6.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 2.300,00')).toBeInTheDocument()
    expect(screen.getByText('Limpeza')).toBeInTheDocument()
  })

  it('exibe mensagem de erro quando a busca falha', async () => {
    buscarRelatorioPeriodo.mockRejectedValueOnce(new Error('Acesso negado a este condomínio.'))
    render(<PrestacaoContasUnidade condominioId={1} />)

    expect(await screen.findByText('Acesso negado a este condomínio.')).toBeInTheDocument()
  })

  it('mostra os valores zerados em vez da mensagem de relatório ainda não publicado', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioZerado)
    render(<PrestacaoContasUnidade condominioId={1} />)

    await screen.findByText('Prestação de contas — 11/2026')
    expect(screen.queryByText('A prestação de contas deste mês ainda não foi publicada.')).not.toBeInTheDocument()
    expect(screen.getAllByText('R$ 0,00').length).toBeGreaterThan(0)
  })
})