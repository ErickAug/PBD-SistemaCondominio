import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MinhasReservas from '../MinhasReservas.jsx'
import {
  listarAreasComuns,
  criarReserva,
  listarReservasDaUnidade,
  cancelarReserva,
  buscarCalendarioReservas,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarAreasComuns: vi.fn(),
  criarReserva: vi.fn(),
  listarReservasDaUnidade: vi.fn(),
  cancelarReserva: vi.fn(),
  buscarCalendarioReservas: vi.fn(),
}))

const areas = [{ id: 3, nome: 'Salão de festas' }]

const reservaFutura = {
  id: 1,
  areaComumNome: 'Salão de festas',
  data: '2099-01-10',
  horaInicio: '19:00:00',
  horaFim: '23:00:00',
  finalidade: 'Aniversário',
  situacao: 'CONFIRMADA',
  motivoRecusa: null,
}

const reservaPassada = {
  ...reservaFutura,
  id: 2,
  data: '2000-01-10',
}

const reservaRecusada = {
  ...reservaFutura,
  id: 3,
  situacao: 'RECUSADA',
  motivoRecusa: 'Já existe outra reserva nesse horário.',
}

beforeEach(() => {
  vi.clearAllMocks()
  listarAreasComuns.mockResolvedValue(areas)
  listarReservasDaUnidade.mockResolvedValue([])
  buscarCalendarioReservas.mockResolvedValue([])
})

async function preencherCamposBasicos() {
  const selectArea = (await screen.findAllByLabelText('Área comum'))[0]
  fireEvent.change(selectArea, { target: { value: '3' } })
  fireEvent.change(screen.getByLabelText('Data'), { target: { value: '2099-01-10' } })
  fireEvent.change(screen.getByLabelText('Início'), { target: { value: '19:00' } })
  fireEvent.change(screen.getByLabelText('Fim'), { target: { value: '23:00' } })
  fireEvent.change(screen.getByLabelText('Finalidade'), { target: { value: 'Aniversário' } })
}

describe('MinhasReservas (morador/proprietário)', () => {
  it('mostra aviso e não chama a API quando não há bloco ou unidade identificados', async () => {
    render(<MinhasReservas condominioId={1} blocoId={null} unidadeId={null} />)

    expect(
      await screen.findByText(
        'Não foi possível identificar a sua unidade. Se você acabou de ser cadastrado, peça ao síndico para registrar a sua entrada.',
      ),
    ).toBeInTheDocument()
    expect(listarReservasDaUnidade).not.toHaveBeenCalled()
  })

  it('carrega as reservas da unidade do usuário logado', async () => {
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)

    await waitFor(() => {
      expect(listarReservasDaUnidade).toHaveBeenCalledWith(1, 2, 10)
    })
  })

  it('exige área, data, horário e finalidade antes de reservar', async () => {
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)
    const botao = await screen.findByRole('button', { name: 'Reservar' })

    fireEvent.submit(botao.closest('form'))

    expect(await screen.findByText('Selecione a área comum.')).toBeInTheDocument()
    expect(criarReserva).not.toHaveBeenCalled()
  })

  it('cria uma reserva com os dados do formulário', async () => {
    criarReserva.mockResolvedValueOnce({})
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)
    await screen.findByRole('button', { name: 'Reservar' })
    await preencherCamposBasicos()

    fireEvent.click(screen.getByRole('button', { name: 'Reservar' }))

    await waitFor(() => {
      expect(criarReserva).toHaveBeenCalledWith(1, 2, 10, {
        areaComumId: 3,
        data: '2099-01-10',
        horaInicio: '19:00',
        horaFim: '23:00',
        finalidade: 'Aniversário',
      })
    })
  })

  it('permite cancelar uma reserva ativa e futura', async () => {
    listarReservasDaUnidade.mockResolvedValueOnce([reservaFutura])
    cancelarReserva.mockResolvedValueOnce({})
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)

    await screen.findByText('Aniversário')
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => {
      expect(cancelarReserva).toHaveBeenCalledWith(1, 2, 10, 1)
    })
  })

  it('não mostra o botão de cancelar para uma reserva com data no passado', async () => {
    listarReservasDaUnidade.mockResolvedValueOnce([reservaPassada])
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)

    await screen.findByText('Aniversário')
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  })

  it('mostra o motivo da recusa para uma reserva recusada', async () => {
    listarReservasDaUnidade.mockResolvedValueOnce([reservaRecusada])
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)

    await screen.findByText('Aniversário')
    expect(screen.getByText('Já existe outra reserva nesse horário.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  })

  it('carrega o calendário de reservas do mês atual', async () => {
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} />)

    await waitFor(() => {
      expect(buscarCalendarioReservas).toHaveBeenCalledWith(1, expect.stringMatching(/^\d{4}-\d{2}$/), null)
    })
  })

  it('não permite reservar nem lista as reservas quando o usuário não mora na unidade', async () => {
    render(<MinhasReservas condominioId={1} blocoId={2} unidadeId={10} podeUsar={false} />)

    expect(
      await screen.findByText('Você não mora nesta unidade, então não pode reservar áreas comuns por ela.'),
    ).toBeInTheDocument()
    expect(listarReservasDaUnidade).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: 'Reservar' })).not.toBeInTheDocument()
  })
})
