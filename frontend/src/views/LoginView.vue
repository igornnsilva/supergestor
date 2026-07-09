<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { LogIn } from '@lucide/vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const error = ref('')

const form = reactive({
  email: 'admin@supergestor.local',
  senha: '123456'
})

async function submit() {
  loading.value = true
  error.value = ''
  try {
    await auth.login({
      email: form.email.trim(),
      senha: form.senha
    })
    router.push('/')
  } catch (exception) {
    if (exception.code === 'ECONNABORTED') {
      error.value = 'O servidor demorou para responder. Aguarde alguns segundos e tente novamente.'
    } else if (!exception.response) {
      error.value = 'Nao foi possivel conectar ao servidor. Tente novamente em instantes.'
    } else {
      error.value = exception.response?.data?.detail ?? 'Email ou senha invalidos.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-screen">
    <section class="login-panel">
      <div class="brand login-brand">
        <span class="brand-mark">S</span>
        <div>
          <strong>SuperGestor</strong>
          <small>Acesso administrativo</small>
        </div>
      </div>

      <form class="grid" @submit.prevent="submit">
        <label>
          Email
          <input v-model="form.email" autocomplete="username" type="email" required />
        </label>
        <label>
          Senha
          <input v-model="form.senha" autocomplete="current-password" type="password" required />
        </label>
        <p v-if="error" class="toast error">{{ error }}</p>
        <button class="button" type="submit" :disabled="loading">
          <LogIn :size="17" />
          {{ loading ? 'Entrando...' : 'Entrar' }}
        </button>
      </form>
    </section>
  </main>
</template>
