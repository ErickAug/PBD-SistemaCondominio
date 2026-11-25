import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import AreasComuns from '../AreasComuns.jsx'
import {
  cadastrarAreaComum,
  listarAreasComuns,
  listarReservasPendentes,
  aprovarReserva,
  recusarReserva,
  buscarCalendarioReservas,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  cadastrarAreaComum: vi.fn(),
  listarAreasComuns: vi.fn(),
  listarReservasPendentes: vi.fn(),
  aprovarReserva: vi.fn(),
  recusarReserva: vi.fn(),
  buscarCalendarioReservas: vi.fn(),
}))

const reservaPendente = {
  id: 7,
  areaComumNome: 'Salão de festas',
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  data: '2099-01-10',
  horaInicio: '19:00:00',
  horaFim: '23:00:00',
  finalidade: 'Aniversário',
  situacao: 'PENDENTE',
  criadoPorNome: 'Maria',
}

beforeEach(() => {
  vi.clearAllMocks()
  listarAreasComuns.mockResolvedValue([])
  listarReservasPendentes.mockResolvedValue([])
  buscarCalendarioReservas.mockResolvedValue([])
})

async function preencherCamposBasicos() {
  fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Salão de festas' } })
  fireEvent.change(screen.getByLabelText('Capacidade'), { target: { value: '50' } })
  fireEvent.change(screen.getByLabelText('Abre às'), { target: { value: '08:00' } })
  fireEvent.change(screen.getByLabelText('Fecha às'), { target: { value: '22:00' } })
}

describe('AreasComuns (síndico)', () => {
  it('exige nome, capacidade e horário antes de cadastrar', async () => {
    render(<AreasComuns condominioId={1} />)
    const botao = await screen.findByRole('button', { name: 'Cadastrar área' })

    fireEvent.submit(botao.closest('form'))

    expect(await screen.findByText('Informe o nome da área comum.')).toBeInTheDocument()
    expect(cadastrarAreaComum).not.toHaveBeenCalled()
  })

  it('cadastra uma área comum com os dados do formulário', async () => {
    cadastrarAreaComum.mockResolvedValueOnce({})
    render(<AreasComuns condominioId={1} />)
    await screen.findByRole('button', { name: 'Cadastrar área' })
    await preencherCamposBasicos()

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar área' }))

    await waitFor(() => {
      expect(cadastrarAreaComum).toHaveBeenCalledWith(1, {
        nome: 'Salão de festas',
        capacidade: 50,
        horarioAbertura: '08:00',
        horarioFechamento: '22:00',
        exigeAprovacao: false,
      })
    })
  })

  it('cadastra uma área marcando que exige aprovação', async () => {
    cadastrarAreaComum.mockResolvedValueOnce({})
    render(<AreasComuns condominioId={1} />)
    await screen.findByRole('button', { name: 'Cadastrar área' })
    await preencherCamposBasicos()
    fireEvent.click(screen.getByLabelText('Exige aprovação do síndico'))

    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar área' }))

    await waitFor(() => {
      expect(cadastrarAreaComum).toHaveBeenCalledWith(1, expect.objectContaining({ exigeAprovacao: true }))
    })
  })

  it('lista as áreas cadastradas', async () => {
    listarAreasComuns.mockResolvedValue([
      { id: 1, nome: 'Salão de festas', capacidade: 50, horarioAbertura: '08:00:00', horarioFechamento: '22:00:00', exigeAprovacao: true },
    ])
    render(<AreasComuns condominioId={1} />)

    expect(await screen.findAllByText('Salão de festas')).not.toHaveLength(0)
    expect(screen.getByText('Exige aprovação')).toBeInTheDocument()
  })

  it('aprova uma reserva pendente', async () => {
    listarReservasPendentes.mockResolvedValueOnce([reservaPendente])
    aprovarReserva.mockResolvedValueOnce({})
    render(<AreasComuns condominioId={1} />)

    await screen.findByText('Aniversário')
    fireEvent.click(screen.getByRole('button', { name: 'Aprovar' }))

    await waitFor(() => {
      expect(aprovarReserva).toHaveBeenCalledWith(1, 7)
    })
  })

  it('exige motivo para recusar uma reserva pendente', async () => {
    listarReservasPendentes.mockResolvedValueOnce([reservaPendente])
    render(<AreasComuns condominioId={1} />)

    await screen.findByText('Aniversário')
    fireEvent.click(screen.getByRole('button', { name: 'Recusar' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar recusa' }))

    expect(await screen.findByText('Informe o motivo da recusa.')).toBeInTheDocument()
    expect(recusarReserva).not.toHaveBeenCalled()
  })

  it('recusa uma reserva pendente com o motivo informado', async () => {
    listarReservasPendentes.mockResolvedValueOnce([reservaPendente])
    recusarReserva.mockResolvedValueOnce({})
    render(<AreasComuns condominioId={1} />)

    await screen.findByText('Aniversário')
    fireEvent.click(screen.getByRole('button', { name: 'Recusar' }))
    fireEvent.change(screen.getByLabelText('Motivo da recusa'), {
      target: { value: 'Já existe outra reserva nesse horário.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar recusa' }))

    await waitFor(() => {
      expect(recusarReserva).toHaveBeenCalledWith(1, 7, 'Já existe outra reserva nesse horário.')
    })
  })

  it('carrega o calendário de reservas do mês atual', async () => {
    render(<AreasComuns condominioId={1} />)

    await waitFor(() => {
      expect(buscarCalendarioReservas).toHaveBeenCalledWith(1, expect.stringMatching(/^\d{4}-\d{2}$/), null)
    })
  })
})
