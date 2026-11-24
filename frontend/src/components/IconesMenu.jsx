
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

export function IconeSair(props) {
  return (
    <svg {...propsBase} {...props}>
      <path d="M8 3.2H4.4a1 1 0 0 0-1 1v9.6a1 1 0 0 0 1 1H8" pathLength="1" />
      <path d="M10.8 9h6.2m0 0l-2.7-2.7M17 9l-2.7 2.7" pathLength="1" />
    </svg>
  )
}
