import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import BlocosUnidades from '../BlocosUnidades.jsx'
import {
  cadastrarBloco,
  listarBlocos,
  cadastrarUnidade,
  listarUnidades,
  excluirUnidade,
  buscarStatusFracaoIdeal,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  cadastrarBloco: vi.fn(),
  listarBlocos: vi.fn(),
  cadastrarUnidade: vi.fn(),
  listarUnidades: vi.fn(),
  excluirUnidade: vi.fn(),
  buscarStatusFracaoIdeal: vi.fn(),
}))

const blocos = [{ id: 1, nome: 'Bloco A' }]

const statusFracaoCompleta = { somaAtual: 100, diferencaParaFechar: 0, fechaEm100: true }

const unidadeComProprietarioEArea = {
  id: 1,
  blocoId: 1,
  numero: '101',
  andar: 1,
  area: 50.5,
  fracaoIdeal: 10,
  proprietarioId: 7,
  tipoOcupacaoAtual: null,
  ocupada: false,
}

const unidadeSemProprietarioNemArea = {
  id: 2,
  blocoId: 1,
  numero: '102',
  andar: 1,
  area: null,
  fracaoIdeal: 10,
  proprietarioId: null,
  tipoOcupacaoAtual: null,
  ocupada: false,
}

beforeEach(() => {
  vi.clearAllMocks()
  listarBlocos.mockResolvedValue(blocos)
  buscarStatusFracaoIdeal.mockResolvedValue(statusFracaoCompleta)
})

describe('BlocosUnidades — proprietário e área', () => {
  it('mostra "—" na área quando a unidade não tem área informada, e o valor normal quando tem', async () => {
    listarUnidades.mockResolvedValueOnce([unidadeComProprietarioEArea, unidadeSemProprietarioNemArea])
    render(<BlocosUnidades nomeCondominio="Meu condomínio" condominioId={1} />)

    expect(await screen.findByText('50.5 m²')).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('mostra selo "Sem proprietário" e "Definido" conforme cada unidade', async () => {
    listarUnidades.mockResolvedValueOnce([unidadeComProprietarioEArea, unidadeSemProprietarioNemArea])
    render(<BlocosUnidades nomeCondominio="Meu condomínio" condominioId={1} />)

    expect(await screen.findByText('Definido')).toBeInTheDocument()
    expect(screen.getByText('Sem proprietário')).toBeInTheDocument()
  })

  it('mostra um aviso com a contagem quando há unidades sem proprietário cadastrado', async () => {
    listarUnidades.mockResolvedValueOnce([unidadeComProprietarioEArea, unidadeSemProprietarioNemArea])
    render(<BlocosUnidades nomeCondominio="Meu condomínio" condominioId={1} />)

    expect(
      await screen.findByText((texto) => texto.includes('sem') && texto.includes('proprietário cadastrado')),
    ).toBeInTheDocument()
  })

  it('não mostra o aviso de proprietário quando todas as unidades já têm proprietário', async () => {
    listarUnidades.mockResolvedValueOnce([unidadeComProprietarioEArea])
    render(<BlocosUnidades nomeCondominio="Meu condomínio" condominioId={1} />)

    await screen.findByText('Definido')
    expect(screen.queryByText(/sem proprietário cadastrado/)).not.toBeInTheDocument()
  })

  it('cadastra uma unidade sem informar a área, enviando null em vez de zero', async () => {
    listarUnidades.mockResolvedValue([])
    cadastrarUnidade.mockResolvedValueOnce({})
    render(<BlocosUnidades nomeCondominio="Meu condomínio" condominioId={1} />)

    await screen.findByText('Nova unidade')

    fireEvent.change(screen.getByLabelText('Bloco'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Número'), { target: { value: '103' } })
    fireEvent.change(screen.getByLabelText('Andar'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Fração ideal (%)'), { target: { value: '10' } })

    fireEvent.click(screen.getByRole('button', { name: 'Adicionar unidade' }))

    await waitFor(() => {
      expect(cadastrarUnidade).toHaveBeenCalledWith(1, '1', {
        numero: '103',
        andar: 1,
        area: null,
        fracaoIdeal: 10,
      })
    })
  })
})
