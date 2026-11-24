import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MeusAvisos from '../MeusAvisos.jsx'
import { listarMeusAvisos, confirmarLeituraAviso } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarMeusAvisos: vi.fn(),
  confirmarLeituraAviso: vi.fn(),
}))

const avisoNaoLido = {
  id: 5,
  titulo: 'Manutenção do elevador',
  texto: 'O elevador do bloco A ficará parado na quinta.',
  alcance: 'BLOCO',
  dataValidade: '2099-01-01',
  autorNome: 'Síndico Carlos',
  dataCriacao: '2026-11-20T10:00:00',
  lido: false,
  dataLeitura: null,
}

beforeEach(() => {
  vi.clearAllMocks()
  listarMeusAvisos.mockResolvedValue([])
})

describe('MeusAvisos (morador/proprietário)', () => {
  it('lista os avisos recebidos', async () => {
    listarMeusAvisos.mockResolvedValueOnce([avisoNaoLido])
    render(<MeusAvisos condominioId={1} />)

    expect(await screen.findByText('Manutenção do elevador')).toBeInTheDocument()
    expect(screen.getByText('O elevador do bloco A ficará parado na quinta.')).toBeInTheDocument()
    expect(screen.getByText('Síndico Carlos')).toBeInTheDocument()
  })

  it('confirma a leitura de um aviso e recarrega a lista', async () => {
    listarMeusAvisos.mockResolvedValueOnce([avisoNaoLido])
    listarMeusAvisos.mockResolvedValueOnce([
      { ...avisoNaoLido, lido: true, dataLeitura: '2026-11-21T09:00:00' },
    ])
    confirmarLeituraAviso.mockResolvedValueOnce({})
    render(<MeusAvisos condominioId={1} />)

    await screen.findByText('Manutenção do elevador')
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar leitura' }))

    await waitFor(() => {
      expect(confirmarLeituraAviso).toHaveBeenCalledWith(1, 5)
    })
    expect(await screen.findByText(/Lido em/)).toBeInTheDocument()
  })

  it('não mostra botão de confirmar leitura para um aviso já lido', async () => {
    listarMeusAvisos.mockResolvedValueOnce([
      { ...avisoNaoLido, lido: true, dataLeitura: '2026-11-21T09:00:00' },
    ])
    render(<MeusAvisos condominioId={1} />)

    await screen.findByText('Manutenção do elevador')
    expect(screen.queryByRole('button', { name: 'Confirmar leitura' })).not.toBeInTheDocument()
    expect(screen.getByText(/Lido em/)).toBeInTheDocument()
  })

  it('mostra erro quando a confirmação de leitura falha', async () => {
    listarMeusAvisos.mockResolvedValue([avisoNaoLido])
    confirmarLeituraAviso.mockRejectedValueOnce(new Error('Este aviso está vencido.'))
    render(<MeusAvisos condominioId={1} />)

    await screen.findByText('Manutenção do elevador')
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar leitura' }))

    expect(await screen.findByText('Este aviso está vencido.')).toBeInTheDocument()
  })
})
