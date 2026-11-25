import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import CadastroCondominios from '../CadastroCondominios.jsx'
import { criarCondominio } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  criarCondominio: vi.fn(),
}))

function preencherCamposObrigatorios(cnpjDigitado) {
  fireEvent.change(screen.getByLabelText('Nome do condomínio'), { target: { value: 'Residencial Teste' } })
  fireEvent.change(screen.getByLabelText('Endereço'), { target: { value: 'Rua Exemplo, 123' } })
  fireEvent.change(screen.getByLabelText('Cidade'), { target: { value: 'Recife' } })
  fireEvent.change(screen.getByLabelText('UF'), { target: { value: 'PE' } })
  fireEvent.change(screen.getByLabelText('CNPJ'), { target: { value: cnpjDigitado } })
}

describe('CadastroCondominios — máscara e validação de CNPJ', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('aplica a máscara 00.000.000/0000-00 enquanto o usuário digita', () => {
    render(<CadastroCondominios administradoraId={1} condominios={[]} setCondominios={() => {}} />)

    fireEvent.change(screen.getByLabelText('CNPJ'), { target: { value: '11222333000181' } })

    expect(screen.getByLabelText('CNPJ')).toHaveValue('11.222.333/0001-81')
  })

  it('ignora caracteres não numéricos e limita a 14 dígitos', () => {
    render(<CadastroCondominios administradoraId={1} condominios={[]} setCondominios={() => {}} />)

    fireEvent.change(screen.getByLabelText('CNPJ'), { target: { value: '11.222.333/0001-8199' } })

    expect(screen.getByLabelText('CNPJ')).toHaveValue('11.222.333/0001-81')
  })

  it('bloqueia o envio e mostra erro quando o CNPJ está incompleto', async () => {
    render(<CadastroCondominios administradoraId={1} condominios={[]} setCondominios={() => {}} />)

    preencherCamposObrigatorios('111')
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar condomínio' }))

    expect(await screen.findByText('CNPJ inválido. Informe os 14 dígitos do CNPJ.')).toBeInTheDocument()
    expect(criarCondominio).not.toHaveBeenCalled()
  })

  it('envia somente os dígitos do CNPJ para a API ao cadastrar', async () => {
    criarCondominio.mockResolvedValueOnce({ id: 1, nome: 'Residencial Teste', cnpj: '11222333000181' })
    const setCondominios = vi.fn()

    render(<CadastroCondominios administradoraId={1} condominios={[]} setCondominios={setCondominios} />)

    preencherCamposObrigatorios('11222333000181')
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar condomínio' }))

    await waitFor(() => {
      expect(criarCondominio).toHaveBeenCalledWith(1, expect.objectContaining({ cnpj: '11222333000181' }))
    })
  })

  it('exibe o CNPJ já formatado na listagem', () => {
    const condominios = [
      { id: 1, nome: 'Residencial Teste', endereco: 'Rua Exemplo, 123', cnpj: '11222333000181', situacao: 'ATIVO' },
    ]

    render(<CadastroCondominios administradoraId={1} condominios={condominios} setCondominios={() => {}} />)

    expect(screen.getByText('11.222.333/0001-81')).toBeInTheDocument()
  })
})
