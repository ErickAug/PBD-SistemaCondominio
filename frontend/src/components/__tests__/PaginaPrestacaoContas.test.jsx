import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PaginaPrestacaoContas from '../PaginaPrestacaoContas.jsx'
import { buscarRelatorioPeriodo, baixarRelatorioCsv } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  buscarRelatorioPeriodo: vi.fn(),
  baixarRelatorioCsv: vi.fn(),
}))

const relatorioComDados = {
  periodo: {
    competencia: '2026-11-01',
    receita: 15000,
    totalDespesa: 9000,
    saldo: 6000,
    despesasPorCategoria: [
      { categoria: 'Limpeza', valor: 4000 },
      { categoria: 'Manutenção', valor: 5000 },
    ],
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

function selecionarMes(valor) {
  const input = screen.getByLabelText('Competência')
  fireEvent.change(input, { target: { value: valor } })
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('PaginaPrestacaoContas', () => {
  it('mantém o botão de gerar relatório desabilitado sem mês selecionado', () => {
    render(<PaginaPrestacaoContas condominioId={1} />)
    expect(screen.getByRole('button', { name: 'Gerar relatório' })).toBeDisabled()
  })

  it('busca o relatório com a competência no formato esperado pelo backend', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioComDados)
    render(<PaginaPrestacaoContas condominioId={1} />)

    selecionarMes('2026-11')
    fireEvent.click(screen.getByRole('button', { name: 'Gerar relatório' }))

    await waitFor(() => {
      expect(buscarRelatorioPeriodo).toHaveBeenCalledWith(1, '2026-11-01')
    })
  })

  it('exibe os valores do período e a comparação com o mês anterior', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioComDados)
    render(<PaginaPrestacaoContas condominioId={1} />)

    selecionarMes('2026-11')
    fireEvent.click(screen.getByRole('button', { name: 'Gerar relatório' }))

    expect(await screen.findByText('R$ 15.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 9.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 6.000,00')).toBeInTheDocument()
    expect(screen.getByText('R$ 2.300,00')).toBeInTheDocument()
    expect(screen.getByText(/\+R\$ 1\.000,00 vs\. 10\/2026/)).toBeInTheDocument()
    expect(screen.getByText('Limpeza')).toBeInTheDocument()
    expect(screen.getByText('Manutenção')).toBeInTheDocument()
  })

  it('não quebra quando o relatório vem todo zerado', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioZerado)
    render(<PaginaPrestacaoContas condominioId={1} />)

    selecionarMes('2026-11')
    fireEvent.click(screen.getByRole('button', { name: 'Gerar relatório' }))

    await waitFor(() => expect(buscarRelatorioPeriodo).toHaveBeenCalled())
    expect(screen.getByText('Nenhuma despesa no período.')).toBeInTheDocument()
  })

  it('exibe mensagem de erro quando a busca do relatório falha', async () => {
    buscarRelatorioPeriodo.mockRejectedValueOnce(new Error('Condomínio não encontrado.'))
    render(<PaginaPrestacaoContas condominioId={999} />)

    selecionarMes('2026-11')
    fireEvent.click(screen.getByRole('button', { name: 'Gerar relatório' }))

    expect(await screen.findByText('Condomínio não encontrado.')).toBeInTheDocument()
  })

  it('aciona o download do CSV com o nome de arquivo correto', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioComDados)
    const blobFake = new Blob(['a,b,c'], { type: 'text/csv' })
    baixarRelatorioCsv.mockResolvedValueOnce(blobFake)

    const criarObjectURL = vi.fn(() => 'blob:fake-url')
    const revogarObjectURL = vi.fn()
    window.URL.createObjectURL = criarObjectURL
    window.URL.revokeObjectURL = revogarObjectURL

    render(<PaginaPrestacaoContas condominioId={1} />)
    selecionarMes('2026-11')
    fireEvent.click(screen.getByRole('button', { name: 'Gerar relatório' }))
    await screen.findByText('Exportar CSV')

    fireEvent.click(screen.getByRole('button', { name: 'Exportar CSV' }))

    await waitFor(() => {
      expect(baixarRelatorioCsv).toHaveBeenCalledWith(1, '2026-11-01')
    })
    expect(criarObjectURL).toHaveBeenCalledWith(blobFake)
    expect(revogarObjectURL).toHaveBeenCalledWith('blob:fake-url')
  })

  it('exibe mensagem de erro quando a exportação do CSV falha', async () => {
    buscarRelatorioPeriodo.mockResolvedValueOnce(relatorioComDados)
    baixarRelatorioCsv.mockRejectedValueOnce(new Error('Não foi possível exportar o CSV do relatório.'))

    render(<PaginaPrestacaoContas condominioId={1} />)
    selecionarMes('2026-11')
    fireEvent.click(screen.getByRole('button', { name: 'Gerar relatório' }))
    await screen.findByText('Exportar CSV')

    fireEvent.click(screen.getByRole('button', { name: 'Exportar CSV' }))

    expect(await screen.findByText('Não foi possível exportar o CSV do relatório.')).toBeInTheDocument()
  })
})