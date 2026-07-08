<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { Boxes, ChartNoAxesCombined, LogOut, PackageSearch, ShoppingCart } from '@lucide/vue'
import { useNotificationsStore } from './stores/notifications'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const notifications = useNotificationsStore()
const auth = useAuthStore()

const navItems = [
  { to: '/', label: 'Dashboard', icon: ChartNoAxesCombined },
  { to: '/produtos', label: 'Produtos', icon: PackageSearch },
  { to: '/estoque', label: 'Estoque', icon: Boxes },
  { to: '/vendas', label: 'Vendas', icon: ShoppingCart }
]

const currentTitle = computed(() => navItems.find((item) => item.to === route.path)?.label ?? 'SuperGestor')
const isLoginRoute = computed(() => route.name === 'login')

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <RouterView v-if="isLoginRoute" />

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">S</span>
        <div>
          <strong>SuperGestor</strong>
          <small>Mercado & estoque</small>
        </div>
      </div>

      <nav class="nav-list" aria-label="Navegacao principal">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: route.path === item.to }"
        >
          <component :is="item.icon" :size="19" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <span class="eyebrow">Sistema administrativo</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="session-actions">
          <div class="operator-pill">{{ auth.user?.nome }} | {{ auth.user?.papel }}</div>
          <button class="icon-button" type="button" aria-label="Sair" @click="logout">
            <LogOut :size="18" />
          </button>
        </div>
      </header>

      <p v-if="notifications.message" class="toast" :class="notifications.type">
        {{ notifications.message }}
      </p>

      <RouterView />
    </main>
  </div>
</template>
