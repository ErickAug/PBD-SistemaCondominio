import { render, screen, within, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import FilaOcorrenciasMorador from '../FilaOcorrenciasMorador.jsx'
import {
  listarFilaOcorrenciasMorador,
  detalharOcorrenciaMorador,
  responderOcorrenciaMorador,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarFilaOcorrenciasMorador: vi.fn(),
  detalharOcorrenciaMorador: vi.fn(),
  responderOcorrenciaMorador: vi.fn(),
}))

const ocorrenciaAberta = {
  id: 9,
  unidadeId: 10,
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  assunto: 'Vazamento na garagem',
  tipo: 'DEFEITO',
  situacao: 'ABERTA',
  temFoto: false,
  abertoPorNome: 'Maria',
  dataAbertura: '2026-11-20T10:00:00',
  respondidoPorNome: null,
  ultimaResposta: null,
  dataUltimaResposta: null,
}

const ocorrenciaResolvida = {
  ...ocorrenciaAberta,
  id: 11,
  assunto: 'Barulho no salão',
  situacao: 'RESOLVIDA',
  respondidoPorNome: 'Síndico Carlos',
  ultimaResposta: 'Conversamos com o morador.',
}

beforeEach(() => {
  vi.clearAllMocks()
  listarFilaOcorrenciasMorador.mockResolvedValue([])
})

describe('FilaOcorrenciasMorador (síndico)', () => {
  it('lista a fila com unidade, assunto, tipo e situação', async () => {
    listarFilaOcorrenciasMorador.mockResolvedValueOnce([ocorrenciaAberta])
    render(<FilaOcorrenciasMorador condominioId={1} />)

    expect(await screen.findByText('Vazamento na garagem')).toBeInTheDocument()
    expect(screen.getByText('Bloco A, 101')).toBeInTheDocument()
    expect(screen.getByText('Defeito')).toBeInTheDocument()
    expect(within(screen.getByRole('table')).getByText('Aberta')).toBeInTheDocument()
  })

  it('carrega a fila sem filtro de situação ao montar', async () => {
    render(<FilaOcorrenciasMorador condominioId={1} />)

    await waitFor(() => {
      expect(listarFilaOcorrenciasMorador).toHaveBeenCalledWith(1, null)
    })
  })

  it('filtra a fila por situação', async () => {
    render(<FilaOcorrenciasMorador condominioId={1} />)
    await waitFor(() => expect(listarFilaOcorrenciasMorador).toHaveBeenCalledTimes(1))

    fireEvent.change(screen.getByLabelText('Situação'), { target: { value: 'RESOLVIDA' } })

    await waitFor(() => {
      expect(listarFilaOcorrenciasMorador).toHaveBeenCalledWith(1, 'RESOLVIDA')
    })
  })

  it('abre o detalhe e permite responder uma ocorrência aberta', async () => {
    listarFilaOcorrenciasMorador.mockResolvedValueOnce([ocorrenciaAberta])
    detalharOcorrenciaMorador.mockResolvedValueOnce({
      id: 9,
      blocoNome: 'Bloco A',
      unidadeNumero: '101',
      abertoPorNome: 'Maria',
      assunto: 'Vazamento na garagem',
      descricao: 'Tem água acumulando perto da vaga 12.',
      foto: null,
      situacao: 'ABERTA',
      historico: [
        { situacaoAnterior: null, situacaoNova: 'ABERTA', resposta: null, responsavelNome: 'Maria', dataHora: '2026-11-20T10:00:00' },
      ],
    })
    responderOcorrenciaMorador.mockResolvedValueOnce({
      id: 9,
      situacao: 'EM_ANALISE',
      historico: [],
    })

    render(<FilaOcorrenciasMorador condominioId={1} />)
    await screen.findByText('Vazamento na garagem')
    fireEvent.click(screen.getByRole('button', { name: 'Responder' }))

    expect(await screen.findByText('Tem água acumulando perto da vaga 12.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Nova situação'), { target: { value: 'EM_ANALISE' } })
    fireEvent.change(screen.getByLabelText('Resposta para o morador'), {
      target: { value: 'Já chamamos a manutenção.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar resposta' }))

    await waitFor(() => {
      expect(responderOcorrenciaMorador).toHaveBeenCalledWith(1, 9, {
        situacao: 'EM_ANALISE',
        resposta: 'Já chamamos a manutenção.',
      })
    })
  })

  it('exige situação e resposta antes de enviar', async () => {
    listarFilaOcorrenciasMorador.mockResolvedValueOnce([ocorrenciaAberta])
    detalharOcorrenciaMorador.mockResolvedValueOnce({
      id: 9,
      blocoNome: 'Bloco A',
      unidadeNumero: '101',
      abertoPorNome: 'Maria',
      assunto: 'Vazamento na garagem',
      descricao: 'Tem água acumulando perto da vaga 12.',
      foto: null,
      situacao: 'ABERTA',
      historico: [],
    })

    render(<FilaOcorrenciasMorador condominioId={1} />)
    await screen.findByText('Vazamento na garagem')
    fireEvent.click(screen.getByRole('button', { name: 'Responder' }))
    await screen.findByText('Tem água acumulando perto da vaga 12.')

    fireEvent.click(screen.getByRole('button', { name: 'Enviar resposta' }))

    expect(await screen.findByText('Selecione a nova situação.')).toBeInTheDocument()
    expect(responderOcorrenciaMorador).not.toHaveBeenCalled()
  })

  it('não mostra o formulário de resposta para uma ocorrência já encerrada', async () => {
    listarFilaOcorrenciasMorador.mockResolvedValueOnce([ocorrenciaResolvida])
    detalharOcorrenciaMorador.mockResolvedValueOnce({
      id: 11,
      blocoNome: 'Bloco A',
      unidadeNumero: '101',
      abertoPorNome: 'Maria',
      assunto: 'Barulho no salão',
      descricao: 'Barulho alto depois das 22h.',
      foto: null,
      situacao: 'RESOLVIDA',
      historico: [
        { situacaoAnterior: 'ABERTA', situacaoNova: 'RESOLVIDA', resposta: 'Conversamos com o morador.', responsavelNome: 'Síndico Carlos', dataHora: '2026-11-21T09:00:00' },
      ],
    })

    render(<FilaOcorrenciasMorador condominioId={1} />)
    await screen.findByText('Barulho no salão')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))

    await screen.findByText('Barulho alto depois das 22h.')
    expect(screen.queryByLabelText('Nova situação')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar resposta' })).not.toBeInTheDocument()
  })
})
