import { useEffect } from 'react'

function Modal({ titulo, aoFechar, children, className = '' }) {
  useEffect(() => {
    function aoPressionarTecla(evento) {
      if (evento.key === 'Escape') aoFechar()
    }
    document.addEventListener('keydown', aoPressionarTecla)
    return () => document.removeEventListener('keydown', aoPressionarTecla)
  }, [aoFechar])

  return (
    <div className="modal-fundo" onClick={aoFechar}>
      <div className={`modal-cartao ${className}`.trim()} onClick={(evento) => evento.stopPropagation()}>
        <div className="modal-cabecalho">
          <h2>{titulo}</h2>
          <button type="button" className="modal-fechar" onClick={aoFechar} aria-label="Fechar">
            ×
          </button>
        </div>
        <div className="modal-conteudo">{children}</div>
      </div>
    </div>
  )
}

export default Modal