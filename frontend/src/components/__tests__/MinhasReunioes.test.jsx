import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MinhasReunioes from '../MinhasReunioes.jsx'
import { listarReunioes, detalharReuniao } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarReunioes: vi.fn(),
  detalharReuniao: vi.fn(),
}))

const reuniaoConvocada = {
  id: 5,
  titulo: 'Assembleia geral ordinária',
  data: '2026-12-10',
  hora: '19:00:00',
  local: 'Salão de festas',
  situacao: 'CONVOCADA',
}

const detalheRealizada = {
  id: 5,
  titulo: 'Assembleia geral ordinária',
  data: '2026-12-10',
  hora: '19:00:00',
  local: 'Salão de festas',
  situacao: 'REALIZADA',
  convocadaPorNome: 'Síndico Carlos',
  dataPublicacao: '2026-11-20T10:00:00',
  pauta: [
    { ordem: 1, descricao: 'Aprovação do orçamento' },
    { ordem: 2, descricao: 'Eleição do síndico' },
  ],
  presencas: [{ id: 1, blocoNome: 'Bloco A', unidadeNumero: '101', compareceu: 'Maria Silva' }],
  unidadesPresentes: 1,
  unidadesDoCondominio: 10,
  ata: {
    texto: 'Orçamento aprovado por unanimidade.',
    temArquivo: true,
    arquivo: 'data:application/pdf;base64,AAAA',
    anexadaPorNome: 'Síndico Carlos',
    dataAnexo: '2026-12-10T21:00:00',
  },
}

beforeEach(() => {
  vi.clearAllMocks()
  listarReunioes.mockResolvedValue([])
})

describe('MinhasReunioes (morador/proprietário)', () => {
  it('lista as reuniões convocadas para o condomínio', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    render(<MinhasReunioes condominioId={1} />)

    expect(await screen.findByText('Assembleia geral ordinária')).toBeInTheDocument()
    expect(screen.getByText('Salão de festas')).toBeInTheDocument()
    expect(screen.getByText('Convocada')).toBeInTheDocument()
  })

  it('carrega as reuniões do condomínio sem filtro de período ao montar', async () => {
    render(<MinhasReunioes condominioId={1} />)

    await waitFor(() => {
      expect(listarReunioes).toHaveBeenCalledWith(1, null, null)
    })
  })

  it('filtra o histórico por período', async () => {
    render(<MinhasReunioes condominioId={1} />)
    await waitFor(() => expect(listarReunioes).toHaveBeenCalledTimes(1))

    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-01-01' } })
    fireEvent.change(screen.getByLabelText('Até'), { target: { value: '2026-12-31' } })

    await waitFor(() => {
      expect(listarReunioes).toHaveBeenCalledWith(1, '2026-01-01', '2026-12-31')
    })
  })

  it('abre o detalhe com pauta, presenças e a ata disponível', async () => {
    listarReunioes.mockResolvedValueOnce([{ ...reuniaoConvocada, situacao: 'REALIZADA' }])
    detalharReuniao.mockResolvedValueOnce(detalheRealizada)

    render(<MinhasReunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))

    expect(await screen.findByText('Aprovação do orçamento')).toBeInTheDocument()
    expect(screen.getByText('Eleição do síndico')).toBeInTheDocument()
    expect(screen.getByText('Bloco A, 101 — Maria Silva')).toBeInTheDocument()
    expect(screen.getByText('Orçamento aprovado por unanimidade.')).toBeInTheDocument()
    expect(screen.getByText('Baixar anexo')).toBeInTheDocument()
  })

  it('mostra que a ata ainda não está disponível para uma reunião apenas convocada', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    detalharReuniao.mockResolvedValueOnce({
      ...detalheRealizada,
      situacao: 'CONVOCADA',
      presencas: [],
      unidadesPresentes: 0,
      ata: null,
    })

    render(<MinhasReunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))

    expect(await screen.findByText('Ata ainda não disponível.')).toBeInTheDocument()
    expect(screen.getByText('Nenhuma presença registrada ainda.')).toBeInTheDocument()
  })
})
