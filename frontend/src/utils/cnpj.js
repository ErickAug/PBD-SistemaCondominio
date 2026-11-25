/**
 * Utilitários de formatação e validação de CNPJ usados nos formulários
 * de cadastro de condomínio e de administradora.
 */

export function somenteDigitos(valor) {
  return (valor ?? '').replace(/\D/g, '')
}

/**
 * Aplica a máscara 00.000.000/0000-00 progressivamente, conforme o
 * usuário digita. Sempre limita a 14 dígitos (tamanho de um CNPJ).
 */
export function formatarCnpj(valor) {
  const digitos = somenteDigitos(valor).slice(0, 14)

  if (digitos.length <= 2) return digitos
  if (digitos.length <= 5) return `${digitos.slice(0, 2)}.${digitos.slice(2)}`
  if (digitos.length <= 8) return `${digitos.slice(0, 2)}.${digitos.slice(2, 5)}.${digitos.slice(5)}`
  if (digitos.length <= 12) {
    return `${digitos.slice(0, 2)}.${digitos.slice(2, 5)}.${digitos.slice(5, 8)}/${digitos.slice(8)}`
  }
  return `${digitos.slice(0, 2)}.${digitos.slice(2, 5)}.${digitos.slice(5, 8)}/${digitos.slice(8, 12)}-${digitos.slice(12)}`
}

/**
 * Verdadeiro quando o valor, ignorando a máscara, tem os 14 dígitos
 * esperados de um CNPJ. Não valida os dígitos verificadores.
 */
export function cnpjTemTamanhoValido(valor) {
  return somenteDigitos(valor).length === 14
}
