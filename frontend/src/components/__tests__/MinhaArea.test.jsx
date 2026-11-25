import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import MinhaArea from '../MinhaArea.jsx'
import { buscarPainelUnidade } from '../../services/api.js'

vi.mock('../../services/api.js', () => ({
  buscarPainelUnidade: vi.fn(),
}))

const unidadeMoradorEDono = {
  unidadeId: 10,
  blocoId: 2,
  blocoNome: 'Bloco A',
  unidadeNumero: '101',
  papeis: ['MORADOR', 'PROPRIETARIO'],
}

const unidadeSoDono = {
  unidadeId: 20,
  blocoId: 3,
  blocoNome: 'Bloco B',
  unidadeNumero: '202',
  papeis: ['PROPRIETARIO'],
}

const painelBase = {
  permissoes: { autorizarVisita: true, reservarAreaComum: true, abrirOcorrencia: true, verPrestacaoContas: true },
  avisos: [
    {
      id: 1, titulo: 'Manutenção do elevador', texto: 'Elevador parado na quinta.', alcance: 'CONDOMINIO',
      dataValidade: '2099-01-01', autorNome: 'Síndico Carlos', dataCriacao: '2026-11-20T10:00:00',
      confirmavel: true, lido: false, dataLeitura: null,
    },
  ],
  cobrancas: [
    {
      cobrancaId: 5, competencia: '2026-10-01', vencimento: '2026-10-10', situacao: 'ABERTA',
      emAtraso: true, diasAtraso: 15, parteOrdinaria: 300, parteExtraordinaria: 50, valorTotal: 350,
      dataPagamento: null,
    },
  ],
  situacaoFinanceira: { emDia: false, cobrancasEmAtraso: 1, valorEmAtraso: 350 },
  reservas: [
    {
      id: 7, areaComumNome: 'Salão de festas', data: '2026-12-10', horaInicio: '19:00:00', horaFim: '23:00:00',
      finalidade: 'Aniversário', situacao: 'CONFIRMADA',
    },
  ],
  ocorrencias: [
    { id: 3, assunto: 'Vazamento na garagem', situacao: 'ABERTA', dataAbertura: '2026-11-20T10:00:00' },
  ],
  competenciaPrestacaoContas: '2026-10-01',
  prestacaoContas: {
    periodo: { competencia: '2026-10-01', receita: 10000, totalDespesa: 8000, saldo: 2000, despesasPorCategoria: [] },
    mesAnterior: { competencia: '2026-09-01', receita: 9000, totalDespesa: 7000, saldo: 2000, despesasPorCategoria: [] },
    inadimplenciaAcumulada: 1200,
  },
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('MinhaArea (morador/proprietário)', () => {
  it('mostra que a unidade ainda está carregando', () => {
    render(<MinhaArea condominioId={1} unidadeAtiva={null} carregandoUnidades />)

    expect(screen.getByText('Carregando...')).toBeInTheDocument()
  })

  it('mostra aviso quando não há nenhuma unidade identificada', () => {
    render(<MinhaArea condominioId={1} unidadeAtiva={null} carregandoUnidades={false} />)

    expect(
      screen.getByText(
        'Não foi possível identificar nenhuma unidade sua. Se você acabou de ser cadastrado, peça ao síndico para registrar a sua entrada.',
      ),
    ).toBeInTheDocument()
    expect(buscarPainelUnidade).not.toHaveBeenCalled()
  })

  it('carrega o painel da unidade ativa e mostra avisos, situação financeira, reservas e ocorrências', async () => {
    buscarPainelUnidade.mockResolvedValueOnce(painelBase)
    render(<MinhaArea condominioId={1} unidadeAtiva={unidadeMoradorEDono} carregandoUnidades={false} />)

    await waitFor(() => {
      expect(buscarPainelUnidade).toHaveBeenCalledWith(1, 10, expect.stringMatching(/^\d{4}-\d{2}-01$/))
    })

    expect(await screen.findByText('Manutenção do elevador')).toBeInTheDocument()
    expect(screen.getByText('Em atraso')).toBeInTheDocument()
    expect(screen.getByText('Salão de festas')).toBeInTheDocument()
    expect(screen.getByText('Vazamento na garagem')).toBeInTheDocument()
  })

  it('mostra que a unidade não pode reservar quando o papel é só proprietário', async () => {
    buscarPainelUnidade.mockResolvedValueOnce({
      ...painelBase,
      permissoes: { ...painelBase.permissoes, autorizarVisita: false, reservarAreaComum: false },
      reservas: null,
    })
    render(<MinhaArea condominioId={1} unidadeAtiva={unidadeSoDono} carregandoUnidades={false} />)

    expect(
      await screen.findByText('Você não mora nesta unidade, então não pode reservar áreas comuns por ela.'),
    ).toBeInTheDocument()
  })

  it('muda o mês da prestação de contas e recarrega o painel com a nova competência', async () => {
    buscarPainelUnidade.mockResolvedValueOnce(painelBase)
    buscarPainelUnidade.mockResolvedValueOnce({
      ...painelBase,
      prestacaoContas: { ...painelBase.prestacaoContas, periodo: { ...painelBase.prestacaoContas.periodo, competencia: '2026-09-01' } },
    })

    render(<MinhaArea condominioId={1} unidadeAtiva={unidadeMoradorEDono} carregandoUnidades={false} />)
    await screen.findByText('Manutenção do elevador')

    fireEvent.change(screen.getByLabelText('Mês'), { target: { value: '2026-09' } })

    await waitFor(() => {
      expect(buscarPainelUnidade).toHaveBeenCalledWith(1, 10, '2026-09-01')
    })
  })
})
