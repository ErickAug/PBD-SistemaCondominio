import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import PortariaRegistros from '../PortariaRegistros.jsx'
import {
  listarBlocos,
  listarUnidades,
  registrarEntradaManual,
  registrarSaidaAcesso,
  listarQuemEstaDentro,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  listarBlocos: vi.fn(),
  listarUnidades: vi.fn(),
  registrarEntradaManual: vi.fn(),
  registrarSaidaAcesso: vi.fn(),
  listarQuemEstaDentro: vi.fn(),
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
}

beforeEach(() => {
  vi.clearAllMocks()
  listarBlocos.mockResolvedValue(blocos)
  listarUnidades.mockResolvedValue(unidades)
  listarQuemEstaDentro.mockResolvedValue([])
})

async function preencherFormulario() {
  await screen.findByRole('option', { name: 'Bloco A' })
  fireEvent.change(screen.getByLabelText('Bloco'), { target: { value: '1' } })
  await screen.findByRole('option', { name: '101' })
  fireEvent.change(screen.getByLabelText('Unidade'), { target: { value: '10' } })
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

    await screen.findByRole('option', { name: 'Bloco A' })
    fireEvent.change(screen.getByLabelText('Bloco'), { target: { value: '1' } })

    await waitFor(() => {
      expect(listarUnidades).toHaveBeenCalledWith(1, '1')
    })
    expect(await screen.findByRole('option', { name: '101' })).toBeInTheDocument()
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
      { ...registroDentro, horaEntrada: new Date(Date.now() - 13 * 3600000).toISOString() },
    ])
    render(<PortariaRegistros condominioId={1} />)

    expect(await screen.findByText('Demorado')).toBeInTheDocument()
  })
})
