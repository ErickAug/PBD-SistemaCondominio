import { render, screen, within, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import LivroOcorrencias from '../LivroOcorrencias.jsx'
import {
  listarBlocos,
  listarUnidades,
  registrarOcorrencia,
  corrigirOcorrencia,
  resolverOcorrencia,
  filtrarOcorrencias,
  encerrarTurno,
  buscarUltimaPassagemTurno,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarBlocos: vi.fn(),
  listarUnidades: vi.fn(),
  registrarOcorrencia: vi.fn(),
  corrigirOcorrencia: vi.fn(),
  resolverOcorrencia: vi.fn(),
  filtrarOcorrencias: vi.fn(),
  encerrarTurno: vi.fn(),
  buscarUltimaPassagemTurno: vi.fn(),
}))

const blocos = [{ id: 1, nome: 'Bloco A' }]
const unidades = [{ id: 10, numero: '101' }]

const ocorrenciaPendente = {
  id: 7,
  unidadeId: 10,
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  texto: 'Portão da garagem travou ao fechar',
  tipo: 'Portão com defeito',
  autorNome: 'Porteiro Noturno',
  dataHora: '2026-11-20T22:10:00',
  correcaoDeId: null,
  resolvida: false,
  resolvidoPorNome: null,
  resolvidoEm: null,
}

beforeEach(() => {
  vi.clearAllMocks()
  listarBlocos.mockResolvedValue(blocos)
  listarUnidades.mockResolvedValue(unidades)
  filtrarOcorrencias.mockResolvedValue([])
  buscarUltimaPassagemTurno.mockResolvedValue(null)
})

async function preencherNovaAnotacao() {
  fireEvent.change(screen.getByPlaceholderText('Ex: Barulho'), {
    target: { value: 'Barulho' },
  })
  fireEvent.change(screen.getByLabelText('O que aconteceu'), {
    target: { value: 'Festa alta no salão de festas' },
  })
}

describe('LivroOcorrencias (portaria)', () => {
  it('exige texto e tipo antes de registrar a anotação', async () => {
    render(<LivroOcorrencias condominioId={1} />)
    const botaoRegistrar = await screen.findByRole('button', { name: 'Registrar anotação' })

    // Os campos têm `required` nativo, então um clique comum é bloqueado pela
    // validação do próprio navegador antes de chegar ao handler. Disparamos o
    // evento de submit diretamente para exercitar a validação feita em JS.
    fireEvent.submit(botaoRegistrar.closest('form'))

    expect(await screen.findByText('Descreva o que aconteceu.')).toBeInTheDocument()
    expect(registrarOcorrencia).not.toHaveBeenCalled()
  })

  it('registra a anotação com os dados do formulário, incluindo a unidade opcional', async () => {
    registrarOcorrencia.mockResolvedValueOnce({})
    render(<LivroOcorrencias condominioId={1} />)
    await preencherNovaAnotacao()

    const selectBloco = screen.getByLabelText('Bloco (opcional)')
    await within(selectBloco).findByRole('option', { name: 'Bloco A' })
    fireEvent.change(selectBloco, { target: { value: '1' } })
    const selectUnidade = screen.getByLabelText('Unidade (opcional)')
    await within(selectUnidade).findByRole('option', { name: '101' })
    fireEvent.change(selectUnidade, { target: { value: '10' } })

    fireEvent.click(screen.getByRole('button', { name: 'Registrar anotação' }))

    await waitFor(() => {
      expect(registrarOcorrencia).toHaveBeenCalledWith(1, {
        texto: 'Festa alta no salão de festas',
        tipo: 'Barulho',
        unidadeId: 10,
      })
    })
  })

  it('registra a anotação sem unidade quando nenhuma é selecionada', async () => {
    registrarOcorrencia.mockResolvedValueOnce({})
    render(<LivroOcorrencias condominioId={1} />)
    await preencherNovaAnotacao()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar anotação' }))

    await waitFor(() => {
      expect(registrarOcorrencia).toHaveBeenCalledWith(1, {
        texto: 'Festa alta no salão de festas',
        tipo: 'Barulho',
        unidadeId: null,
      })
    })
  })

  it('não permite editar ou excluir uma anotação existente — só corrigir', async () => {
    filtrarOcorrencias.mockResolvedValueOnce([ocorrenciaPendente])
    render(<LivroOcorrencias condominioId={1} />)

    await screen.findByText('Portão da garagem travou ao fechar')

    expect(screen.queryByRole('button', { name: /editar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /excluir/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Corrigir' })).toBeInTheDocument()
  })

  it('corrige uma anotação registrando uma nova, ligada à original', async () => {
    filtrarOcorrencias.mockResolvedValueOnce([ocorrenciaPendente])
    corrigirOcorrencia.mockResolvedValueOnce({})
    render(<LivroOcorrencias condominioId={1} />)

    await screen.findByText('Portão da garagem travou ao fechar')
    fireEvent.click(screen.getByRole('button', { name: 'Corrigir' }))

    const textarea = screen.getByLabelText('Nova redação')
    expect(textarea.value).toBe('Portão da garagem travou ao fechar')
    fireEvent.change(textarea, {
      target: { value: 'Portão da garagem travou ao abrir, não ao fechar' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))

    await waitFor(() => {
      expect(corrigirOcorrencia).toHaveBeenCalledWith(
        1,
        7,
        'Portão da garagem travou ao abrir, não ao fechar'
      )
    })
  })

  it('marca uma ocorrência como resolvida', async () => {
    filtrarOcorrencias.mockResolvedValueOnce([ocorrenciaPendente])
    resolverOcorrencia.mockResolvedValueOnce({})
    render(<LivroOcorrencias condominioId={1} />)

    await screen.findByText('Portão da garagem travou ao fechar')
    fireEvent.click(screen.getByRole('button', { name: 'Resolver' }))

    await waitFor(() => {
      expect(resolverOcorrencia).toHaveBeenCalledWith(1, 7)
    })
  })

  it('não mostra o botão de resolver para uma ocorrência já resolvida, e exibe quem e quando', async () => {
    filtrarOcorrencias.mockResolvedValueOnce([
      {
        ...ocorrenciaPendente,
        resolvida: true,
        resolvidoPorNome: 'Síndico Carlos',
        resolvidoEm: '2026-11-21T08:00:00',
      },
    ])
    render(<LivroOcorrencias condominioId={1} />)

    await screen.findByText('Portão da garagem travou ao fechar')
    expect(screen.queryByRole('button', { name: 'Resolver' })).not.toBeInTheDocument()
    expect(screen.getByText(/Resolvida/)).toBeInTheDocument()
    expect(screen.getByText(/Síndico Carlos/)).toBeInTheDocument()
  })

  it('mostra as pendências do turno anterior ao carregar a tela', async () => {
    buscarUltimaPassagemTurno.mockResolvedValueOnce({
      id: 3,
      porteiroNome: 'Porteiro Noturno',
      horarioEncerramento: '2026-11-20T06:00:00',
      pendencias: [
        { id: 7, tipo: 'Portão com defeito', texto: 'Portão da garagem travou', unidadeId: null },
      ],
    })
    render(<LivroOcorrencias condominioId={1} />)

    expect(
      await screen.findByText('Turno anterior encerrado por Porteiro Noturno', { exact: false })
    ).toBeInTheDocument()
    expect(screen.getByText('1 pendência(s) deixada(s):')).toBeInTheDocument()
    expect(screen.getByText(/Portão da garagem travou/)).toBeInTheDocument()
  })

  it('avisa quando não há pendências do turno anterior', async () => {
    buscarUltimaPassagemTurno.mockResolvedValueOnce({
      id: 3,
      porteiroNome: 'Porteiro Noturno',
      horarioEncerramento: '2026-11-20T06:00:00',
      pendencias: [],
    })
    render(<LivroOcorrencias condominioId={1} />)

    expect(await screen.findByText('Nenhuma pendência deixada.')).toBeInTheDocument()
  })

  it('encerra o turno após confirmação', async () => {
    encerrarTurno.mockResolvedValueOnce({})
    render(<LivroOcorrencias condominioId={1} />)
    await screen.findByRole('button', { name: 'Encerrar turno' })

    fireEvent.click(screen.getByRole('button', { name: 'Encerrar turno' }))
    expect(screen.getByText('Confirma o encerramento do turno agora?')).toBeInTheDocument()
    expect(encerrarTurno).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }))

    await waitFor(() => {
      expect(encerrarTurno).toHaveBeenCalledWith(1)
    })
  })

  it('cancela o encerramento do turno sem chamar a API', async () => {
    render(<LivroOcorrencias condominioId={1} />)
    await screen.findByRole('button', { name: 'Encerrar turno' })

    fireEvent.click(screen.getByRole('button', { name: 'Encerrar turno' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByText('Confirma o encerramento do turno agora?')).not.toBeInTheDocument()
    expect(encerrarTurno).not.toHaveBeenCalled()
  })

  it('filtra o livro por período, bloco, unidade e tipo', async () => {
    filtrarOcorrencias.mockResolvedValue([])
    render(<LivroOcorrencias condominioId={1} />)
    await screen.findAllByRole('option', { name: 'Bloco A' })

    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-11-01' } })
    fireEvent.change(screen.getByLabelText('Até'), { target: { value: '2026-11-30' } })
    fireEvent.change(screen.getByLabelText('Bloco'), { target: { value: '1' } })
    await screen.findAllByRole('option', { name: '101' })
    fireEvent.change(screen.getByLabelText('Unidade'), { target: { value: '10' } })
    fireEvent.change(screen.getByPlaceholderText('Todos'), { target: { value: 'Barulho' } })

    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    await waitFor(() => {
      expect(filtrarOcorrencias).toHaveBeenCalledWith(1, {
        inicio: '2026-11-01',
        fim: '2026-11-30',
        tipo: 'Barulho',
        unidadeId: '10',
      })
    })
  })
})

describe('LivroOcorrencias (síndico, somente leitura)', () => {
  it('não mostra formulário de nova anotação nem o encerramento de turno', async () => {
    render(<LivroOcorrencias condominioId={1} somenteLeitura />)
    await screen.findByRole('button', { name: 'Filtrar' })

    expect(screen.queryByText('Nova anotação')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar anotação' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Encerrar turno' })).not.toBeInTheDocument()
    expect(buscarUltimaPassagemTurno).not.toHaveBeenCalled()
  })

  it('não mostra ações de corrigir ou resolver, só a leitura', async () => {
    filtrarOcorrencias.mockResolvedValueOnce([ocorrenciaPendente])
    render(<LivroOcorrencias condominioId={1} somenteLeitura />)

    await screen.findByText('Portão da garagem travou ao fechar')
    expect(screen.queryByRole('button', { name: 'Corrigir' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Resolver' })).not.toBeInTheDocument()
  })

  it('ainda permite filtrar o livro', async () => {
    filtrarOcorrencias.mockResolvedValue([])
    render(<LivroOcorrencias condominioId={1} somenteLeitura />)
    await screen.findAllByRole('option', { name: 'Bloco A' })

    fireEvent.change(screen.getByLabelText('Bloco'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    await waitFor(() => {
      expect(filtrarOcorrencias).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ unidadeId: null })
      )
    })
  })
})
