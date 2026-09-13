const USUARIOS_FAKE = [
  {
    usuario: 'admin',
    senha: '123456',
    perfil: 'administradora',
    nome: 'Ana (Condovix Administração)',
    condominios: ['Residencial Jardim das Flores', 'Edifício Monte Verde'],
  },
  {
    usuario: 'sindico.jardim',
    senha: '123456',
    perfil: 'sindico',
    nome: 'Carlos Silva',
    condominios: ['Residencial Jardim das Flores'],
  },
  {
    usuario: 'portaria.jardim',
    senha: '123456',
    perfil: 'portaria',
    nome: 'Portaria - Jardim das Flores',
    condominios: ['Residencial Jardim das Flores'],
  },
  {
    usuario: 'morador.joao',
    senha: '123456',
    perfil: 'morador',
    nome: 'João Pereira',
    condominios: ['Residencial Jardim das Flores'],
  },
]


export function autenticar(usuario, senha) {
  const encontrado = USUARIOS_FAKE.find(
    (u) => u.usuario === usuario && u.senha === senha
  )
  return encontrado || null
}