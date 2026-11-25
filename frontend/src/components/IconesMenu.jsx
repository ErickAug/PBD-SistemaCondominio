
const propsBase = {
  viewBox: '0 0 18 18',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: '1.4',
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
}

export function IconeEmpresa(props) {
  return (
    <svg {...propsBase} {...props}>
      <rect x="2.5" y="6" width="13" height="9" rx="1" pathLength="1" />
      <path d="M6.5 6V4.5a1.5 1.5 0 0 1 1.5-1.5h2a1.5 1.5 0 0 1 1.5 1.5V6" pathLength="1" />
    </svg>
  )
}

export function IconePredio(props) {
  return (
    <svg {...propsBase} {...props}>
      <rect x="3.5" y="2" width="11" height="14" rx="0.5" pathLength="1" />
      <path d="M6 5.5h1M11 5.5h1M6 9h1M11 9h1M6 12.5h1M11 12.5h1" pathLength="1" />
    </svg>
  )
}

export function IconeUsuario(props) {
  return (
    <svg {...propsBase} {...props}>
      <circle cx="9" cy="6" r="2.8" pathLength="1" />
      <path d="M3.2 15.5c0-3.2 2.6-5.7 5.8-5.7s5.8 2.5 5.8 5.7" pathLength="1" />
    </svg>
  )
}

export function IconeDespesa(props) {
  return (
    <svg {...propsBase} {...props}>
      <rect x="2" y="5" width="14" height="8" rx="1.2" pathLength="1" />
      <circle cx="9" cy="9" r="1.8" pathLength="1" />
    </svg>
  )
}

export function IconeBusca(props) {
  return (
    <svg {...propsBase} {...props}>
      <circle cx="7.5" cy="7.5" r="4.3" pathLength="1" />
      <path d="M14.2 14.2l-2.9-2.9" pathLength="1" />
    </svg>
  )
}

export function IconeRateio(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M14 4L4 14" pathLength="1" />
      <circle cx="5.3" cy="5.3" r="1.6" pathLength="1" />
      <circle cx="12.7" cy="12.7" r="1.6" pathLength="1" />
    </svg>
  )
}

export function IconeCobranca(props) {
  return (
    <svg {...propsBase} {...props}>
      <rect x="3.5" y="2" width="11" height="14" rx="1" pathLength="1" />
      <path d="M6.3 9.3l1.8 1.8 3.6-4.2" pathLength="1" />
    </svg>
  )
}

export function IconeRelatorio(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M3.5 15V9.5M9 15V4.5M14.5 15V8" pathLength="1" />
    </svg>
  )
}

export function IconeLivro(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M9 4.2c-1-.9-2.4-1.4-4.2-1.4-.7 0-1.3.6-1.3 1.3v8.9c0 .7.6 1.1 1.3 1.1 1.8 0 3.2.5 4.2 1.4" pathLength="1" />
      <path d="M9 4.2c1-.9 2.4-1.4 4.2-1.4.7 0 1.3.6 1.3 1.3v8.9c0 .7-.6 1.1-1.3 1.1-1.8 0-3.2.5-4.2 1.4V4.2z" pathLength="1" />
    </svg>
  )
}

export function IconeAviso(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M3 7.3v3.4a1 1 0 0 0 1 1h1.3l5.3 3V3.3l-5.3 3H4a1 1 0 0 0-1 1z" pathLength="1" />
      <path d="M12.6 6.6a3.1 3.1 0 0 1 0 4.8" pathLength="1" />
      <path d="M14.5 4.7a5.6 5.6 0 0 1 0 8.6" pathLength="1" />
    </svg>
  )
}

export function IconeChamado(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M2.5 4.5h13a1 1 0 0 1 1 1v6.4a1 1 0 0 1-1 1H8.2L4.8 15v-2.1H2.5a1 1 0 0 1-1-1V5.5a1 1 0 0 1 1-1z" pathLength="1" />
      <path d="M9 7.3v2.6M9 11.3v.1" pathLength="1" />
    </svg>
  )
}

export function IconeReserva(props) {
  return (
    <svg {...propsBase} {...props}>
      <rect x="2.5" y="3.5" width="13" height="12" rx="1" pathLength="1" />
      <path d="M2.5 7h13" pathLength="1" />
      <path d="M5.5 2v3M12.5 2v3" pathLength="1" />
      <path d="M6 10.2l1.7 1.7L12 7.8" pathLength="1" />
    </svg>
  )
}

export function IconeSair(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M8 3.2H4.4a1 1 0 0 0-1 1v9.6a1 1 0 0 0 1 1H8" pathLength="1" />
      <path d="M10.8 9h6.2m0 0l-2.7-2.7M17 9l-2.7 2.7" pathLength="1" />
    </svg>
  )
}
