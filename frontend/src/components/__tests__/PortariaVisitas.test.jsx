import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PortariaVisitas from '../PortariaVisitas.jsx'
import {
  buscarVisitasEsperadas,
  listarVisitasEsperadasHoje,
  registrarEntradaPorAutorizacaoVisita,
  registrarEntradaPorAutorizadoPermanente,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  buscarVisitasEsperadas: vi.fn(),
  listarVisitasEsperadasHoje: vi.fn(),
  registrarEntradaPorAutorizacaoVisita: vi.fn(),
  registrarEntradaPorAutorizadoPermanente: vi.fn(),
}))

const visitaAvulsa = {
  id: 1,
  origem: 'AVULSA',
  nome: 'João',
  documento: '123.456.789-00',
  tipo: 'Entrega',
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  dataInicio: '2026-11-23',
  dataFim: '2026-11-23',
  situacao: 'LIBERADA',
}

const visitaPermanente = {
  id: 2,
  origem: 'PERMANENTE',
  nome: 'Maria',
  documento: '987.654.321-00',
  tipo: 'Prestador de serviço',
  blocoNome: 'Bloco B',
  unidadeNumero: '202',
  dataInicio: '2026-11-01',
  dataFim: '2026-11-01',
  situacao: 'LIBERADA',
}

beforeEach(() => {
  vi.clearAllMocks()
  listarVisitasEsperadasHoje.mockResolvedValue([])
})

describe('PortariaVisitas', () => {
  it('carrega as visitas esperadas hoje ao montar', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([visitaAvulsa])
    render(<PortariaVisitas condominioId={1} />)

    expect(await screen.findByText('João')).toBeInTheDocument()
  })

  it('busca por nome ou documento', async () => {
    buscarVisitasEsperadas.mockResolvedValueOnce([visitaPermanente])
    render(<PortariaVisitas condominioId={1} />)

    fireEvent.change(screen.getByLabelText('Nome ou documento'), { target: { value: 'Maria' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(buscarVisitasEsperadas).toHaveBeenCalledWith(1, 'Maria')
    })
    expect(await screen.findByText('Maria')).toBeInTheDocument()
  })

  it('exibe mensagem quando a busca não encontra nada', async () => {
    buscarVisitasEsperadas.mockResolvedValueOnce([])
    render(<PortariaVisitas condominioId={1} />)

    fireEvent.change(screen.getByLabelText('Nome ou documento'), { target: { value: 'Ninguém' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Nada encontrado para "Ninguém"')).toBeInTheDocument()
  })

  it('registra a entrada de uma visita avulsa pelo botão correspondente', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([visitaAvulsa])
    listarVisitasEsperadasHoje.mockResolvedValueOnce([{ ...visitaAvulsa, situacao: 'USADA' }])
    registrarEntradaPorAutorizacaoVisita.mockResolvedValueOnce({})
    render(<PortariaVisitas condominioId={1} />)

    await screen.findByText('João')
    fireEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }))

    await waitFor(() => {
      expect(registrarEntradaPorAutorizacaoVisita).toHaveBeenCalledWith(1, 1)
    })
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: 'Registrar entrada' })).not.toBeInTheDocument()
    })
  })

  it('registra a entrada de um autorizado permanente pelo botão correspondente', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([visitaPermanente])
    listarVisitasEsperadasHoje.mockResolvedValueOnce([{ ...visitaPermanente, situacao: 'USADA' }])
    registrarEntradaPorAutorizadoPermanente.mockResolvedValueOnce({})
    render(<PortariaVisitas condominioId={1} />)

    await screen.findByText('Maria')
    fireEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }))

    await waitFor(() => {
      expect(registrarEntradaPorAutorizadoPermanente).toHaveBeenCalledWith(1, 2)
    })
  })

  it('não mostra o botão de registrar entrada quando a situação não é liberada', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([{ ...visitaAvulsa, situacao: 'VENCIDA' }])
    render(<PortariaVisitas condominioId={1} />)

    await screen.findByText('João')
    expect(screen.queryByRole('button', { name: 'Registrar entrada' })).not.toBeInTheDocument()
  })
})
