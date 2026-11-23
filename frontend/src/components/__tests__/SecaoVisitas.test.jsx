import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import ListasDaUnidade from '../ListasDaUnidade.jsx'
import {
  listarMoradores,
  listarAutorizacoesVisita,
  cadastrarAutorizacaoVisita,
  cancelarAutorizacaoVisita,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarMoradores: vi.fn(),
  registrarMorador: vi.fn(),
  encerrarVinculoMorador: vi.fn(),
  listarDependentes: vi.fn(),
  cadastrarDependente: vi.fn(),
  desativarDependente: vi.fn(),
  listarAutorizados: vi.fn(),
  cadastrarAutorizado: vi.fn(),
  listarAutorizacoesVisita: vi.fn(),
  cadastrarAutorizacaoVisita: vi.fn(),
  cancelarAutorizacaoVisita: vi.fn(),
  listarVagas: vi.fn(),
  criarVaga: vi.fn(),
  excluirVaga: vi.fn(),
  listarVeiculos: vi.fn(),
  cadastrarVeiculo: vi.fn(),
  desativarVeiculo: vi.fn(),
  listarCobrancasDaUnidade: vi.fn(),
}))

const autorizacaoLiberada = {
  id: 1,
  unidadeId: 5,
  nome: 'João Entregador',
  documento: '123.456.789-00',
  tipo: 'Entrega',
  dataInicio: '2026-11-23',
  dataFim: '2026-11-23',
  status: 'PENDENTE',
  dataUso: null,
  situacao: 'LIBERADA',
}

const autorizacaoCancelada = {
  id: 2,
  unidadeId: 5,
  nome: 'Visita Antiga',
  documento: '000.000.000-00',
  tipo: 'Visita',
  dataInicio: '2026-11-01',
  dataFim: '2026-11-01',
  status: 'CANCELADA',
  dataUso: null,
  situacao: 'CANCELADA',
}

async function abrirAbaVisitas() {
  render(<ListasDaUnidade condominioId={1} blocoId={1} unidadeId={1} />)
  await waitFor(() => expect(listarMoradores).toHaveBeenCalled())
  fireEvent.click(screen.getByRole('button', { name: 'Autorizar visita' }))
}

beforeEach(() => {
  vi.clearAllMocks()
  listarMoradores.mockResolvedValue([])
})

describe('SecaoVisitas (aba Autorizar visita)', () => {
  it('lista as autorizações de visita da unidade ao abrir a aba', async () => {
    listarAutorizacoesVisita.mockResolvedValueOnce([autorizacaoLiberada])

    await abrirAbaVisitas()

    expect(await screen.findByText('João Entregador')).toBeInTheDocument()
    expect(listarAutorizacoesVisita).toHaveBeenCalledWith(1, 1, 1)
    expect(screen.getByText('Liberada')).toBeInTheDocument()
  })

  it('cadastra uma nova autorização com os campos do formulário', async () => {
    listarAutorizacoesVisita.mockResolvedValue([])
    cadastrarAutorizacaoVisita.mockResolvedValueOnce(autorizacaoLiberada)

    await abrirAbaVisitas()
    await screen.findByText('Nenhuma autorização de visita cadastrada')

    fireEvent.change(screen.getByPlaceholderText('Ex: João (entregador)'), {
      target: { value: 'João Entregador' },
    })
    fireEvent.change(screen.getByLabelText('Documento'), { target: { value: '123.456.789-00' } })
    fireEvent.change(screen.getByPlaceholderText('Ex: Visita'), { target: { value: 'Entrega' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar autorização' }))

    await waitFor(() => {
      expect(cadastrarAutorizacaoVisita).toHaveBeenCalledWith(1, 1, 1, {
        nome: 'João Entregador',
        documento: '123.456.789-00',
        tipo: 'Entrega',
        dataInicio: expect.any(String),
        dataFim: null,
      })
    })
  })

  it('permite cancelar uma autorização pendente', async () => {
    listarAutorizacoesVisita.mockResolvedValue([autorizacaoLiberada])
    cancelarAutorizacaoVisita.mockResolvedValueOnce({ ...autorizacaoLiberada, situacao: 'CANCELADA' })

    await abrirAbaVisitas()
    await screen.findByText('João Entregador')

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => {
      expect(cancelarAutorizacaoVisita).toHaveBeenCalledWith(1, 1, 1, 1)
    })
  })

  it('não mostra o botão de cancelar para autorizações já canceladas', async () => {
    listarAutorizacoesVisita.mockResolvedValueOnce([autorizacaoCancelada])

    await abrirAbaVisitas()

    expect(await screen.findByText('Visita Antiga')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument()
  })

  it('exibe mensagem de erro quando o cadastro falha', async () => {
    listarAutorizacoesVisita.mockResolvedValue([])
    cadastrarAutorizacaoVisita.mockRejectedValueOnce(new Error('Não foi possível cadastrar a autorização de visita.'))

    await abrirAbaVisitas()
    await screen.findByText('Nenhuma autorização de visita cadastrada')

    fireEvent.change(screen.getByPlaceholderText('Ex: João (entregador)'), {
      target: { value: 'João Entregador' },
    })
    fireEvent.change(screen.getByLabelText('Documento'), { target: { value: '123.456.789-00' } })
    fireEvent.change(screen.getByPlaceholderText('Ex: Visita'), { target: { value: 'Entrega' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cadastrar autorização' }))

    expect(
      await screen.findByText('Não foi possível cadastrar a autorização de visita.')
    ).toBeInTheDocument()
  })
})
