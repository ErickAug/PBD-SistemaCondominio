import { describe, it, expect } from 'vitest'
import { formatarCnpj, somenteDigitos, cnpjTemTamanhoValido } from '../cnpj.js'

describe('formatarCnpj', () => {
  it('formata progressivamente conforme os dígitos entram', () => {
    expect(formatarCnpj('1')).toBe('1')
    expect(formatarCnpj('11')).toBe('11')
    expect(formatarCnpj('112')).toBe('11.2')
    expect(formatarCnpj('11222')).toBe('11.222')
    expect(formatarCnpj('112223')).toBe('11.222.3')
    expect(formatarCnpj('11222333')).toBe('11.222.333')
    expect(formatarCnpj('112223330')).toBe('11.222.333/0')
    expect(formatarCnpj('112223330001')).toBe('11.222.333/0001')
    expect(formatarCnpj('1122233300018')).toBe('11.222.333/0001-8')
    expect(formatarCnpj('11222333000181')).toBe('11.222.333/0001-81')
  })

  it('ignora caracteres não numéricos', () => {
    expect(formatarCnpj('11.222.333/0001-81')).toBe('11.222.333/0001-81')
  })

  it('limita a 14 dígitos mesmo recebendo mais', () => {
    expect(formatarCnpj('1122233300018199')).toBe('11.222.333/0001-81')
  })
})

describe('somenteDigitos', () => {
  it('remove tudo que não for dígito', () => {
    expect(somenteDigitos('11.222.333/0001-81')).toBe('11222333000181')
  })

  it('lida com valor vazio ou indefinido', () => {
    expect(somenteDigitos('')).toBe('')
    expect(somenteDigitos(undefined)).toBe('')
  })
})

describe('cnpjTemTamanhoValido', () => {
  it('verdadeiro apenas com exatamente 14 dígitos', () => {
    expect(cnpjTemTamanhoValido('11.222.333/0001-81')).toBe(true)
    expect(cnpjTemTamanhoValido('111')).toBe(false)
    expect(cnpjTemTamanhoValido('1111111111111111')).toBe(false)
  })
})
