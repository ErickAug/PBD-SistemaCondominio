import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MinhasOcorrencias from '../MinhasOcorrencias.jsx'
import {
  abrirOcorrenciaMorador,
  listarOcorrenciasDaUnidade,
  detalharOcorrenciaDaUnidade,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  abrirOcorrenciaMorador: vi.fn(),
  listarOcorrenciasDaUnidade: vi.fn(),
  detalharOcorrenciaDaUnidade: vi.fn(),
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

beforeEach(() => {
  vi.clearAllMocks()
  listarOcorrenciasDaUnidade.mockResolvedValue([])
})

async function preencherCampos() {
  fireEvent.change(screen.getByLabelText('Assunto'), { target: { value: 'Vazamento na garagem' } })
  fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'DEFEITO' } })
  fireEvent.change(screen.getByLabelText('Descrição'), {
    target: { value: 'Tem água acumulando perto da vaga 12.' },
  })
}

describe('MinhasOcorrencias (morador/proprietário)', () => {
  it('mostra aviso e não chama a API quando não há bloco ou unidade identificados', async () => {
    render(<MinhasOcorrencias condominioId={1} blocoId={null} unidadeId={null} />)

    expect(
      await screen.findByText('Não foi possível identificar a sua unidade. Se você acabou de ser', { exact: false })
    ).toBeInTheDocument()
    expect(listarOcorrenciasDaUnidade).not.toHaveBeenCalled()
  })

  it('carrega as ocorrências da unidade do usuário logado', async () => {
    render(<MinhasOcorrencias condominioId={1} blocoId={2} unidadeId={10} />)

    await waitFor(() => {
      expect(listarOcorrenciasDaUnidade).toHaveBeenCalledWith(1, 2, 10)
    })
  })

  it('exige assunto, tipo e descrição antes de registrar', async () => {
    render(<MinhasOcorrencias condominioId={1} blocoId={2} unidadeId={10} />)
    const botao = await screen.findByRole('button', { name: 'Registrar ocorrência' })

    fireEvent.submit(botao.closest('form'))

    expect(await screen.findByText('Informe o assunto da ocorrência.')).toBeInTheDocument()
    expect(abrirOcorrenciaMorador).not.toHaveBeenCalled()
  })

  it('registra uma ocorrência sem foto', async () => {
    abrirOcorrenciaMorador.mockResolvedValueOnce({})
    render(<MinhasOcorrencias condominioId={1} blocoId={2} unidadeId={10} />)
    await screen.findByRole('button', { name: 'Registrar ocorrência' })
    await preencherCampos()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar ocorrência' }))

    await waitFor(() => {
      expect(abrirOcorrenciaMorador).toHaveBeenCalledWith(1, 2, 10, {
        assunto: 'Vazamento na garagem',
        tipo: 'DEFEITO',
        descricao: 'Tem água acumulando perto da vaga 12.',
        foto: null,
      })
    })
  })

  it('mostra "Aguardando resposta." quando a ocorrência ainda não foi respondida', async () => {
    listarOcorrenciasDaUnidade.mockResolvedValueOnce([ocorrenciaAberta])
    render(<MinhasOcorrencias condominioId={1} blocoId={2} unidadeId={10} />)

    expect(await screen.findByText('Aguardando resposta.')).toBeInTheDocument()
  })

  it('mostra a última resposta e abre os detalhes com o histórico completo', async () => {
    listarOcorrenciasDaUnidade.mockResolvedValueOnce([
      { ...ocorrenciaAberta, situacao: 'RESOLVIDA', respondidoPorNome: 'Síndico Carlos', ultimaResposta: 'Já conserjamos o cano.' },
    ])
    detalharOcorrenciaDaUnidade.mockResolvedValueOnce({
      id: 9,
      descricao: 'Tem água acumulando perto da vaga 12.',
      foto: null,
      situacao: 'RESOLVIDA',
      historico: [
        { situacaoAnterior: null, situacaoNova: 'ABERTA', resposta: null, responsavelNome: 'Maria', dataHora: '2026-11-20T10:00:00' },
        { situacaoAnterior: 'ABERTA', situacaoNova: 'RESOLVIDA', resposta: 'Já conserjamos o cano.', responsavelNome: 'Síndico Carlos', dataHora: '2026-11-21T09:00:00' },
      ],
    })
    render(<MinhasOcorrencias condominioId={1} blocoId={2} unidadeId={10} />)

    expect(await screen.findByText('Já conserjamos o cano.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))

    await waitFor(() => {
      expect(detalharOcorrenciaDaUnidade).toHaveBeenCalledWith(1, 2, 10, 9)
    })
    expect(await screen.findByText('Tem água acumulando perto da vaga 12.')).toBeInTheDocument()
    expect(screen.getByText('Síndico Carlos', { exact: false })).toBeInTheDocument()
  })
})
