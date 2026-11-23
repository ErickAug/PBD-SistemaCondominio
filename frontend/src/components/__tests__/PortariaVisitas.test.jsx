import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PortariaVisitas from '../PortariaVisitas.jsx'
import { buscarVisitasEsperadas, listarVisitasEsperadasHoje } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  buscarVisitasEsperadas: vi.fn(),
  listarVisitasEsperadasHoje: vi.fn(),
}))

const visitaAvulsaLiberada = {
  origem: 'AVULSA',
  id: 1,
  unidadeId: 10,
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  nome: 'João Entregador',
  documento: '123.456.789-00',
  tipo: 'Entrega',
  dataInicio: '2026-11-23',
  dataFim: '2026-11-23',
  situacao: 'LIBERADA',
}

const autorizadoPermanenteVencido = {
  origem: 'PERMANENTE',
  id: 2,
  unidadeId: 11,
  blocoNome: 'Bloco B',
  unidadeNumero: '202',
  nome: 'Maria Diarista',
  documento: '987.654.321-00',
  tipo: 'Diarista',
  dataInicio: '2026-01-01',
  dataFim: '2026-06-30',
  situacao: 'VENCIDA',
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('PortariaVisitas', () => {
  it('carrega as visitas esperadas hoje automaticamente ao montar', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([visitaAvulsaLiberada])

    render(<PortariaVisitas condominioId={1} />)

    expect(await screen.findByText('João Entregador')).toBeInTheDocument()
    expect(listarVisitasEsperadasHoje).toHaveBeenCalledWith(1)
  })

  it('mostra mensagem quando não há visitas esperadas hoje', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([])

    render(<PortariaVisitas condominioId={1} />)

    expect(await screen.findByText('Nenhuma visita esperada hoje.')).toBeInTheDocument()
  })

  it('busca por nome ou documento e exibe autorizações avulsas e permanentes', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([])
    buscarVisitasEsperadas.mockResolvedValueOnce([visitaAvulsaLiberada, autorizadoPermanenteVencido])

    render(<PortariaVisitas condominioId={1} />)
    await screen.findByText('Nenhuma visita esperada hoje.')

    fireEvent.change(screen.getByLabelText('Nome ou documento'), { target: { value: 'joão' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => expect(buscarVisitasEsperadas).toHaveBeenCalledWith(1, 'joão'))
    expect(await screen.findByText('2 resultado(s) para "joão"')).toBeInTheDocument()
    expect(screen.getByText('Maria Diarista')).toBeInTheDocument()
  })

  it('exibe a situação vencida do autorizado permanente, não liberada', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([])
    buscarVisitasEsperadas.mockResolvedValueOnce([autorizadoPermanenteVencido])

    render(<PortariaVisitas condominioId={1} />)
    await screen.findByText('Nenhuma visita esperada hoje.')

    fireEvent.change(screen.getByLabelText('Nome ou documento'), { target: { value: 'maria' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Vencida')).toBeInTheDocument()
    expect(screen.queryByText('Liberada')).not.toBeInTheDocument()
  })

  it('exibe mensagem de erro quando a busca falha', async () => {
    listarVisitasEsperadasHoje.mockResolvedValueOnce([])
    buscarVisitasEsperadas.mockRejectedValueOnce(new Error('Não foi possível buscar as visitas.'))

    render(<PortariaVisitas condominioId={1} />)
    await screen.findByText('Nenhuma visita esperada hoje.')

    fireEvent.change(screen.getByLabelText('Nome ou documento'), { target: { value: 'x' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Não foi possível buscar as visitas.')).toBeInTheDocument()
  })
})
