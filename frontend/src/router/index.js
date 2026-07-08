import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import ProdutosView from '../views/ProdutosView.vue'
import EstoqueView from '../views/EstoqueView.vue'
import VendasView from '../views/VendasView.vue'
import LoginView from '../views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/produtos', name: 'produtos', component: ProdutosView },
    { path: '/estoque', name: 'estoque', component: EstoqueView },
    { path: '/vendas', name: 'vendas', component: VendasView }
  ]
})

router.beforeEach((to) => {
  const hasToken = Boolean(localStorage.getItem('supergestor_auth'))
  if (!to.meta.public && !hasToken) {
    return '/login'
  }
  if (to.name === 'login' && hasToken) {
    return '/'
  }
  return true
})

export default router
