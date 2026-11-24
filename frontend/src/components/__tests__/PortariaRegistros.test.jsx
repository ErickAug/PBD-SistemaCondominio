import { render, screen, within, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PortariaRegistros from '../PortariaRegistros.jsx'
import {
  listarBlocos,
  listarUnidades,
  registrarEntradaManual,
  registrarSaidaAcesso,
  listarQuemEstaDentro,
  listarQuemEstaDentroNaUnidade,
  filtrarHistoricoAcessos,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarBlocos: vi.fn(),
  listarUnidades: vi.fn(),
  registrarEntradaManual: vi.fn(),
  registrarSaidaAcesso: vi.fn(),
  listarQuemEstaDentro: vi.fn(),
  listarQuemEstaDentroNaUnidade: vi.fn(),
  filtrarHistoricoAcessos: vi.fn(),
}))

const blocos = [{ id: 1, nome: 'Bloco A' }]
const unidades = [{ id: 10, numero: '101' }]

const registroDentro = {
  id: 5,
  nome: 'Carlos',
  documento: '111.222.333-44',
  tipo: 'Visita',
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  horaEntrada: new Date().toISOString(),
  demorado: false,
}

beforeEach(() => {
  vi.clearAllMocks()
  listarBlocos.mockResolvedValue(blocos)
  listarUnidades.mockResolvedValue(unidades)
  listarQuemEstaDentro.mockResolvedValue([])
  listarQuemEstaDentroNaUnidade.mockResolvedValue([])
  filtrarHistoricoAcessos.mockResolvedValue([])
})

async function preencherFormulario() {
  const selectBloco = screen.getByLabelText('Bloco')
  await within(selectBloco).findByRole('option', { name: 'Bloco A' })
  fireEvent.change(selectBloco, { target: { value: '1' } })
  const selectUnidade = screen.getByLabelText('Unidade')
  await within(selectUnidade).findByRole('option', { name: '101' })
  fireEvent.change(selectUnidade, { target: { value: '10' } })
  fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Carlos' } })
  fireEvent.change(screen.getByLabelText('Documento'), { target: { value: '111.222.333-44' } })
  fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'Visita' } })
  fireEvent.change(screen.getByLabelText('Quem autorizou'), { target: { value: 'Síndico' } })
}

describe('PortariaRegistros', () => {
  it('carrega quem está dentro ao montar', async () => {
    listarQuemEstaDentro.mockResolvedValueOnce([registroDentro])
    render(<PortariaRegistros condominioId={1} />)

    expect(await screen.findByText('Carlos')).toBeInTheDocument()
  })

  it('carrega as unidades do bloco selecionado', async () => {
    render(<PortariaRegistros condominioId={1} />)

    const selectBloco = screen.getByLabelText('Bloco')
    await within(selectBloco).findByRole('option', { name: 'Bloco A' })
    fireEvent.change(selectBloco, { target: { value: '1' } })

    await waitFor(() => {
      expect(listarUnidades).toHaveBeenCalledWith(1, '1')
    })
    const selectUnidade = screen.getByLabelText('Unidade')
    expect(await within(selectUnidade).findByRole('option', { name: '101' })).toBeInTheDocument()
  })

  it('exige quem autorizou antes de registrar a entrada', async () => {
    render(<PortariaRegistros condominioId={1} />)
    await preencherFormulario()
    fireEvent.change(screen.getByLabelText('Quem autorizou'), { target: { value: '' } })

    fireEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }))

    expect(
      await screen.findByText('Sem autorização prévia, registre quem, na unidade, liberou a entrada.')
    ).toBeInTheDocument()
    expect(registrarEntradaManual).not.toHaveBeenCalled()
  })

  it('registra a entrada manual com os dados do formulário', async () => {
    registrarEntradaManual.mockResolvedValueOnce({})
    render(<PortariaRegistros condominioId={1} />)
    await preencherFormulario()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }))

    await waitFor(() => {
      expect(registrarEntradaManual).toHaveBeenCalledWith(1, {
        unidadeId: 10,
        nome: 'Carlos',
        documento: '111.222.333-44',
        tipo: 'Visita',
        quemAutorizou: 'Síndico',
        placa: null,
      })
    })
  })

  it('exibe mensagem de erro quando o registro falha', async () => {
    registrarEntradaManual.mockRejectedValueOnce(
      new Error('Esta pessoa já está dentro do condomínio e ainda não teve saída registrada.')
    )
    render(<PortariaRegistros condominioId={1} />)
    await preencherFormulario()

    fireEvent.click(screen.getByRole('button', { name: 'Registrar entrada' }))

    expect(
      await screen.findByText('Esta pessoa já está dentro do condomínio e ainda não teve saída registrada.')
    ).toBeInTheDocument()
  })

  it('registra a saída de quem está dentro', async () => {
    listarQuemEstaDentro.mockResolvedValueOnce([registroDentro])
    registrarSaidaAcesso.mockResolvedValueOnce({})
    render(<PortariaRegistros condominioId={1} />)

    await screen.findByText('Carlos')
    fireEvent.click(screen.getByRole('button', { name: 'Registrar saída' }))

    await waitFor(() => {
      expect(registrarSaidaAcesso).toHaveBeenCalledWith(1, 5)
    })
  })

  it('mostra o selo de demorado quando a entrada passou de 12 horas', async () => {
    listarQuemEstaDentro.mockResolvedValueOnce([
      { ...registroDentro, demorado: true, horaEntrada: new Date(Date.now() - 13 * 3600000).toISOString() },
    ])
    render(<PortariaRegistros condominioId={1} />)

    expect(await screen.findByText('Demorado')).toBeInTheDocument()
  })

  it('busca quem está dentro em uma unidade específica', async () => {
    listarQuemEstaDentroNaUnidade.mockResolvedValueOnce([registroDentro])
    render(<PortariaRegistros condominioId={1} />)

    await screen.findAllByRole('option', { name: 'Bloco A' })
    fireEvent.change(screen.getByLabelText('Bloco para busca'), { target: { value: '1' } })
    await screen.findAllByRole('option', { name: '101' })
    fireEvent.change(screen.getByLabelText('Unidade para busca'), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    await waitFor(() => {
      expect(listarQuemEstaDentroNaUnidade).toHaveBeenCalledWith(1, '10')
    })
    expect(await screen.findByText('Carlos')).toBeInTheDocument()
  })

  it('exige a seleção de unidade antes de buscar', async () => {
    render(<PortariaRegistros condominioId={1} />)
    await screen.findAllByRole('option', { name: 'Bloco A' })
    fireEvent.change(screen.getByLabelText('Bloco para busca'), { target: { value: '1' } })

    fireEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByText('Selecione a unidade para buscar.')).toBeInTheDocument()
    expect(listarQuemEstaDentroNaUnidade).not.toHaveBeenCalled()
  })

  it('filtra o histórico de acessos por período, unidade e tipo e exibe o porteiro da saída', async () => {
    filtrarHistoricoAcessos.mockResolvedValueOnce([
      {
        id: 7,
        nome: 'Maria',
        documento: '555.666.777-88',
        tipo: 'Visita',
        blocoNome: 'Bloco A',
        unidadeNumero: '101',
        horaEntrada: '2026-11-01T10:00:00',
        horaSaida: '2026-11-01T12:00:00',
        porteiroNome: 'Porteiro Entrada',
        porteiroSaidaNome: 'Porteiro Saída',
        demorado: false,
      },
    ])
    render(<PortariaRegistros condominioId={1} />)

    await screen.findAllByRole('option', { name: 'Bloco A' })
    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-11-01' } })
    fireEvent.change(screen.getByLabelText('Até'), { target: { value: '2026-11-30' } })
    fireEvent.change(screen.getByLabelText('Filtrar por bloco'), { target: { value: '1' } })
    await screen.findAllByRole('option', { name: '101' })
    fireEvent.change(screen.getByLabelText('Filtrar por unidade'), { target: { value: '10' } })
    fireEvent.change(screen.getByLabelText('Filtrar por tipo'), { target: { value: 'Visita' } })

    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    await waitFor(() => {
      expect(filtrarHistoricoAcessos).toHaveBeenCalledWith(1, {
        inicio: '2026-11-01',
        fim: '2026-11-30',
        unidadeId: '10',
        tipo: 'Visita',
      })
    })

    expect(await screen.findByText('Maria')).toBeInTheDocument()
    expect(screen.getByText('Porteiro Entrada')).toBeInTheDocument()
    expect(screen.getByText('Porteiro Saída')).toBeInTheDocument()
  })

  it('exibe mensagem de vazio quando nenhum registro corresponde aos filtros do histórico', async () => {
    filtrarHistoricoAcessos.mockResolvedValueOnce([])
    render(<PortariaRegistros condominioId={1} />)

    fireEvent.click(screen.getByRole('button', { name: 'Filtrar' }))

    expect(
      await screen.findByText('Nenhum registro encontrado para os filtros selecionados.')
    ).toBeInTheDocument()
  })
})
