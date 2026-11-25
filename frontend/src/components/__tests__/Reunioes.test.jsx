import { render, screen, within, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Reunioes from '../Reunioes.jsx'
import {
  convocarReuniao,
  listarReunioes,
  detalharReuniao,
  registrarPresencaReuniao,
  anexarAtaReuniao,
  listarBlocos,
  listarUnidades,
} from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  convocarReuniao: vi.fn(),
  listarReunioes: vi.fn(),
  detalharReuniao: vi.fn(),
  registrarPresencaReuniao: vi.fn(),
  anexarAtaReuniao: vi.fn(),
  listarBlocos: vi.fn(),
  listarUnidades: vi.fn(),
}))

const blocos = [{ id: 1, nome: 'Bloco A' }]
const unidades = [{ id: 10, numero: '101' }]

const reuniaoConvocada = {
  id: 5,
  titulo: 'Assembleia geral ordinária',
  data: '2026-12-10',
  hora: '19:00:00',
  local: 'Salão de festas',
  situacao: 'CONVOCADA',
  itensPauta: 2,
  unidadesPresentes: 0,
  temAta: false,
}

const detalheConvocada = {
  id: 5,
  condominioId: 1,
  titulo: 'Assembleia geral ordinária',
  data: '2026-12-10',
  hora: '19:00:00',
  local: 'Salão de festas',
  situacao: 'CONVOCADA',
  convocadaPorNome: 'Síndico Carlos',
  dataPublicacao: '2026-11-20T10:00:00',
  pauta: [
    { ordem: 1, descricao: 'Aprovação do orçamento' },
    { ordem: 2, descricao: 'Eleição do síndico' },
  ],
  presencas: [],
  unidadesPresentes: 0,
  unidadesDoCondominio: 10,
  ata: null,
}

beforeEach(() => {
  vi.clearAllMocks()
  listarReunioes.mockResolvedValue([])
  listarBlocos.mockResolvedValue(blocos)
  listarUnidades.mockResolvedValue(unidades)
})

async function preencherCamposBasicos() {
  fireEvent.change(screen.getByLabelText('Título'), { target: { value: 'Assembleia geral ordinária' } })
  fireEvent.change(screen.getByLabelText('Data'), { target: { value: '2026-12-10' } })
  fireEvent.change(screen.getByLabelText('Hora'), { target: { value: '19:00' } })
  fireEvent.change(screen.getByLabelText('Local'), { target: { value: 'Salão de festas' } })
}

describe('Reunioes (síndico)', () => {
  it('exige título, data, hora e local antes de convocar', async () => {
    render(<Reunioes condominioId={1} />)
    const botao = await screen.findByRole('button', { name: 'Convocar reunião' })

    fireEvent.submit(botao.closest('form'))

    expect(await screen.findByText('Informe o título da reunião.')).toBeInTheDocument()
    expect(convocarReuniao).not.toHaveBeenCalled()
  })

  it('exige ao menos um item de pauta', async () => {
    render(<Reunioes condominioId={1} />)
    await screen.findByRole('button', { name: 'Convocar reunião' })
    await preencherCamposBasicos()

    fireEvent.click(screen.getByRole('button', { name: 'Convocar reunião' }))

    expect(await screen.findByText('Informe a pauta com pelo menos um assunto.')).toBeInTheDocument()
    expect(convocarReuniao).not.toHaveBeenCalled()
  })

  it('convoca uma reunião com título, data, hora, local e pauta ordenada', async () => {
    convocarReuniao.mockResolvedValueOnce({})
    render(<Reunioes condominioId={1} />)
    await screen.findByRole('button', { name: 'Convocar reunião' })
    await preencherCamposBasicos()

    fireEvent.change(screen.getByPlaceholderText('Assunto 1'), { target: { value: 'Aprovação do orçamento' } })
    fireEvent.click(screen.getByRole('button', { name: 'Adicionar item' }))
    fireEvent.change(screen.getByPlaceholderText('Assunto 2'), { target: { value: 'Eleição do síndico' } })

    fireEvent.click(screen.getByRole('button', { name: 'Convocar reunião' }))

    await waitFor(() => {
      expect(convocarReuniao).toHaveBeenCalledWith(1, {
        titulo: 'Assembleia geral ordinária',
        data: '2026-12-10',
        hora: '19:00',
        local: 'Salão de festas',
        pauta: ['Aprovação do orçamento', 'Eleição do síndico'],
      })
    })
  })

  it('lista o histórico de reuniões', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    render(<Reunioes condominioId={1} />)

    expect(await screen.findByText('Assembleia geral ordinária')).toBeInTheDocument()
    expect(screen.getByText('Salão de festas')).toBeInTheDocument()
    expect(screen.getByText('Convocada')).toBeInTheDocument()
  })

  it('abre o detalhe com a pauta ordenada e registra a presença de uma unidade', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    detalharReuniao.mockResolvedValueOnce(detalheConvocada)
    registrarPresencaReuniao.mockResolvedValueOnce({})
    detalharReuniao.mockResolvedValueOnce({
      ...detalheConvocada,
      presencas: [{ id: 1, blocoNome: 'Bloco A', unidadeNumero: '101', compareceu: 'Maria Silva' }],
      unidadesPresentes: 1,
    })

    render(<Reunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))

    expect(await screen.findByText('Aprovação do orçamento')).toBeInTheDocument()
    expect(screen.getByText('Eleição do síndico')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Bloco'), { target: { value: '1' } })
    const selectUnidade = await screen.findByLabelText('Unidade')
    await within(selectUnidade).findByRole('option', { name: '101' })
    fireEvent.change(selectUnidade, { target: { value: '10' } })
    fireEvent.change(screen.getByLabelText('Quem compareceu'), { target: { value: 'Maria Silva' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrar presença' }))

    await waitFor(() => {
      expect(registrarPresencaReuniao).toHaveBeenCalledWith(1, 5, {
        unidadeId: 10,
        compareceu: 'Maria Silva',
      })
    })
    expect(await screen.findByText('Bloco A, 101 — Maria Silva')).toBeInTheDocument()
  })

  it('exige unidade e quem compareceu antes de registrar presença', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    detalharReuniao.mockResolvedValueOnce(detalheConvocada)

    render(<Reunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))
    await screen.findByText('Aprovação do orçamento')

    fireEvent.click(screen.getByRole('button', { name: 'Registrar presença' }))

    expect(await screen.findByText('Selecione a unidade.')).toBeInTheDocument()
    expect(registrarPresencaReuniao).not.toHaveBeenCalled()
  })

  it('exige o texto da ata antes de anexar', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    detalharReuniao.mockResolvedValueOnce(detalheConvocada)

    render(<Reunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))
    await screen.findByText('Aprovação do orçamento')

    fireEvent.click(screen.getByRole('button', { name: 'Anexar ata' }))

    expect(await screen.findByText('Informe o texto da ata com as deliberações.')).toBeInTheDocument()
    expect(anexarAtaReuniao).not.toHaveBeenCalled()
  })

  it('anexa a ata com o texto informado', async () => {
    listarReunioes.mockResolvedValueOnce([reuniaoConvocada])
    detalharReuniao.mockResolvedValueOnce(detalheConvocada)
    anexarAtaReuniao.mockResolvedValueOnce({})
    detalharReuniao.mockResolvedValueOnce({
      ...detalheConvocada,
      situacao: 'REALIZADA',
      ata: {
        texto: 'Orçamento aprovado por unanimidade.',
        temArquivo: false,
        arquivo: null,
        anexadaPorNome: 'Síndico Carlos',
        dataAnexo: '2026-12-10T21:00:00',
      },
    })

    render(<Reunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))
    await screen.findByText('Aprovação do orçamento')

    fireEvent.change(screen.getByPlaceholderText('Descreva as deliberações da reunião'), {
      target: { value: 'Orçamento aprovado por unanimidade.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Anexar ata' }))

    await waitFor(() => {
      expect(anexarAtaReuniao).toHaveBeenCalledWith(1, 5, {
        texto: 'Orçamento aprovado por unanimidade.',
        arquivo: null,
      })
    })
    expect(await screen.findByText('Orçamento aprovado por unanimidade.')).toBeInTheDocument()
  })

  it('não mostra o formulário de ata nem de presença para uma reunião já realizada', async () => {
    listarReunioes.mockResolvedValueOnce([{ ...reuniaoConvocada, situacao: 'REALIZADA' }])
    detalharReuniao.mockResolvedValueOnce({
      ...detalheConvocada,
      situacao: 'REALIZADA',
      ata: {
        texto: 'Orçamento aprovado por unanimidade.',
        temArquivo: false,
        arquivo: null,
        anexadaPorNome: 'Síndico Carlos',
        dataAnexo: '2026-12-10T21:00:00',
      },
    })

    render(<Reunioes condominioId={1} />)
    await screen.findByText('Assembleia geral ordinária')
    fireEvent.click(screen.getByRole('button', { name: 'Ver detalhes' }))

    expect(await screen.findByText('Orçamento aprovado por unanimidade.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Registrar presença' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Anexar ata' })).not.toBeInTheDocument()
  })
})
