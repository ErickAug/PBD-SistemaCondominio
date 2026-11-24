import { render, screen, within, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Avisos from '../Avisos.jsx'
import {
  listarBlocos,
  listarUnidades,
  criarAviso,
  listarHistoricoAvisos,
  listarLeiturasAviso,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarBlocos: vi.fn(),
  listarUnidades: vi.fn(),
  criarAviso: vi.fn(),
  listarHistoricoAvisos: vi.fn(),
  listarLeiturasAviso: vi.fn(),
}))

const blocos = [{ id: 1, nome: 'Bloco A' }]
const unidades = [{ id: 10, numero: '101' }]

const avisoVigente = {
  id: 5,
  condominioId: 1,
  titulo: 'Manutenção do elevador',
  texto: 'O elevador do bloco A ficará parado na quinta.',
  alcance: 'BLOCO',
  blocoId: 1,
  blocoNome: 'Bloco A',
  unidadeId: null,
  unidadeNumero: null,
  dataValidade: '2099-01-01',
  vencido: false,
  autorNome: 'Síndico Carlos',
  dataCriacao: '2026-11-20T10:00:00',
}

beforeEach(() => {
  vi.clearAllMocks()
  listarBlocos.mockResolvedValue(blocos)
  listarUnidades.mockResolvedValue(unidades)
  listarHistoricoAvisos.mockResolvedValue([])
})

async function preencherCamposBasicos() {
  fireEvent.change(screen.getByLabelText('Título'), { target: { value: 'Manutenção do elevador' } })
  fireEvent.change(screen.getByLabelText('Texto'), {
    target: { value: 'O elevador do bloco A ficará parado na quinta.' },
  })
  fireEvent.change(screen.getByLabelText('Validade'), { target: { value: '2099-01-01' } })
}

describe('Avisos (síndico)', () => {
  it('exige título, texto e validade antes de publicar', async () => {
    render(<Avisos condominioId={1} />)
    const botao = await screen.findByRole('button', { name: 'Publicar aviso' })

    fireEvent.submit(botao.closest('form'))

    expect(await screen.findByText('Informe o título do aviso.')).toBeInTheDocument()
    expect(criarAviso).not.toHaveBeenCalled()
  })

  it('cria um aviso para todo o condomínio sem bloco ou unidade', async () => {
    criarAviso.mockResolvedValueOnce({})
    render(<Avisos condominioId={1} />)
    await screen.findByRole('button', { name: 'Publicar aviso' })
    await preencherCamposBasicos()

    expect(screen.queryByLabelText('Bloco')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Unidade')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Publicar aviso' }))

    await waitFor(() => {
      expect(criarAviso).toHaveBeenCalledWith(1, {
        titulo: 'Manutenção do elevador',
        texto: 'O elevador do bloco A ficará parado na quinta.',
        alcance: 'CONDOMINIO',
        blocoId: null,
        unidadeId: null,
        dataValidade: '2099-01-01',
      })
    })
  })

  it('cria um aviso para um bloco específico', async () => {
    criarAviso.mockResolvedValueOnce({})
    render(<Avisos condominioId={1} />)
    await screen.findByRole('button', { name: 'Publicar aviso' })
    await preencherCamposBasicos()

    fireEvent.change(screen.getByLabelText('Alcance'), { target: { value: 'BLOCO' } })
    const selectBloco = await screen.findByLabelText('Bloco')
    await within(selectBloco).findByRole('option', { name: 'Bloco A' })
    fireEvent.change(selectBloco, { target: { value: '1' } })

    fireEvent.click(screen.getByRole('button', { name: 'Publicar aviso' }))

    await waitFor(() => {
      expect(criarAviso).toHaveBeenCalledWith(1, {
        titulo: 'Manutenção do elevador',
        texto: 'O elevador do bloco A ficará parado na quinta.',
        alcance: 'BLOCO',
        blocoId: 1,
        unidadeId: null,
        dataValidade: '2099-01-01',
      })
    })
  })

  it('cria um aviso para uma unidade específica, exigindo bloco e unidade', async () => {
    criarAviso.mockResolvedValueOnce({})
    render(<Avisos condominioId={1} />)
    await screen.findByRole('button', { name: 'Publicar aviso' })
    await preencherCamposBasicos()

    fireEvent.change(screen.getByLabelText('Alcance'), { target: { value: 'UNIDADE' } })
    const selectBloco = await screen.findByLabelText('Bloco')
    await within(selectBloco).findByRole('option', { name: 'Bloco A' })
    fireEvent.change(selectBloco, { target: { value: '1' } })

    const selectUnidade = await screen.findByLabelText('Unidade')
    await within(selectUnidade).findByRole('option', { name: '101' })
    fireEvent.change(selectUnidade, { target: { value: '10' } })

    fireEvent.click(screen.getByRole('button', { name: 'Publicar aviso' }))

    await waitFor(() => {
      expect(criarAviso).toHaveBeenCalledWith(1, {
        titulo: 'Manutenção do elevador',
        texto: 'O elevador do bloco A ficará parado na quinta.',
        alcance: 'UNIDADE',
        blocoId: 1,
        unidadeId: 10,
        dataValidade: '2099-01-01',
      })
    })
  })

  it('mostra a situação de vigente e de vencido no histórico', async () => {
    listarHistoricoAvisos.mockResolvedValueOnce([
      avisoVigente,
      { ...avisoVigente, id: 6, titulo: 'Aviso antigo', vencido: true, alcance: 'CONDOMINIO' },
    ])
    render(<Avisos condominioId={1} />)

    await screen.findByText('Manutenção do elevador')
    expect(screen.getByText('Vigente')).toBeInTheDocument()
    expect(screen.getByText('Vencido')).toBeInTheDocument()
  })

  it('abre o relatório de leituras de um aviso', async () => {
    listarHistoricoAvisos.mockResolvedValueOnce([avisoVigente])
    listarLeiturasAviso.mockResolvedValueOnce({
      avisoId: 5,
      titulo: 'Manutenção do elevador',
      vencido: false,
      totalDestinatarios: 2,
      totalLeram: 1,
      totalNaoLeram: 1,
      leram: [
        { usuarioId: 1, nome: 'Maria', unidadeId: 10, blocoNome: 'Bloco A', unidadeNumero: '101', papel: 'MORADOR', leu: true, dataLeitura: '2026-11-21T09:00:00' },
      ],
      naoLeram: [
        { usuarioId: 2, nome: 'João', unidadeId: 11, blocoNome: 'Bloco A', unidadeNumero: '102', papel: 'PROPRIETARIO', leu: false, dataLeitura: null },
      ],
    })
    render(<Avisos condominioId={1} />)

    await screen.findByText('Manutenção do elevador')
    fireEvent.click(screen.getByRole('button', { name: 'Ver leituras' }))

    expect(await screen.findByText('1 de 2 destinatário(s) leram este aviso.')).toBeInTheDocument()
    expect(screen.getByText('Maria')).toBeInTheDocument()
    expect(screen.getByText('João')).toBeInTheDocument()
  })
})
