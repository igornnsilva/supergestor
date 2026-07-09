import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000
})

api.interceptors.request.use((config) => {
  const savedAuth = localStorage.getItem('supergestor_auth')
  if (savedAuth) {
    const auth = JSON.parse(savedAuth)
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem('supergestor_auth')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (payload) => api.post('/auth/login', payload).then((response) => response.data)
}

export const dashboardApi = {
  resumo: () => api.get('/dashboard/resumo').then((response) => response.data)
}

export const produtoApi = {
  listar: (busca = '') => api.get('/produtos', { params: { busca } }).then((response) => response.data),
  criar: (payload) => api.post('/produtos', payload).then((response) => response.data)
}

export const cadastroApi = {
  categorias: () => api.get('/categorias').then((response) => response.data),
  fornecedores: () => api.get('/fornecedores').then((response) => response.data),
  clientes: () => api.get('/clientes').then((response) => response.data),
  usuarios: () => api.get('/usuarios').then((response) => response.data)
}

export const estoqueApi = {
  baixo: () => api.get('/estoque/baixo').then((response) => response.data),
  movimentacoes: () => api.get('/estoque/movimentacoes').then((response) => response.data),
  entrada: (payload) => api.post('/estoque/entrada', payload).then((response) => response.data)
}

export const vendaApi = {
  listar: () => api.get('/vendas').then((response) => response.data),
  criar: (payload) => api.post('/vendas', payload).then((response) => response.data)
}
