import { useState } from 'react'

const TITULOS_POR_PERFIL = {
  sindico: 'Painel do síndico',
  portaria: 'Painel da portaria',
  morador: 'Área do morador',
  proprietario: 'Área do proprietário',
}

function AreaPerfil({ usuarioLogado }) {
  const [mensagemAcesso, setMensagemAcesso] = useState('')
  const condominio = usuarioLogado.condominios[0]

  function tentarAcessarOutroCondominio() {
    setMensagemAcesso(
      'Acesso negado: você não tem permissão para acessar dados de outro condomínio.'
    )
  }

  return (
    <section>
      <h2>{TITULOS_POR_PERFIL[usuarioLogado.perfil] || 'Painel'}</h2>
      <p className="descricao">
        Você está vendo apenas dados do condomínio <strong>{condominio}</strong>,
        que é o único vinculado ao seu usuário.
      </p>

      <button
        type="button"
        className="botao-secundario"
        onClick={tentarAcessarOutroCondominio}
      >
        Consultar outro condomínio
      </button>

      {mensagemAcesso && <p className="erro-login">{mensagemAcesso}</p>}
    </section>
  )
}

export default AreaPerfil