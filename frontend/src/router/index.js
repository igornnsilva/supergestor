import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import ProdutosView from '../views/ProdutosView.vue'
import EstoqueView from '../views/EstoqueView.vue'
import VendasView from '../views/VendasView.vue'
import LoginView from '../views/LoginView.vue'
import UsuariosView from '../views/UsuariosView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/produtos', name: 'produtos', component: ProdutosView },
    { path: '/estoque', name: 'estoque', component: EstoqueView },
    { path: '/vendas', name: 'vendas', component: VendasView },
    { path: '/usuarios', name: 'usuarios', component: UsuariosView, meta: { roles: ['ADMIN'] } }
  ]
})

router.beforeEach((to) => {
  const savedAuth = localStorage.getItem('supergestor_auth')
  const session = savedAuth ? JSON.parse(savedAuth) : null
  const hasToken = Boolean(session?.token)
  if (!to.meta.public && !hasToken) {
    return '/login'
  }
  if (to.name === 'login' && hasToken) {
    return '/'
  }
  if (to.meta.roles && !to.meta.roles.includes(session?.papel)) {
    return '/'
  }
  return true
})

export default router
