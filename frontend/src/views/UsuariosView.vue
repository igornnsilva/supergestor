<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, Save, Trash2, X } from '@lucide/vue'
import { usuarioApi } from '../services/api'
import { useAuthStore } from '../stores/auth'
import { useNotificationsStore } from '../stores/notifications'

const auth = useAuthStore()
const notifications = useNotificationsStore()
const usuarios = ref([])
const usuarioEmEdicaoId = ref(null)

const papeis = [
  'ADMIN',
  'GERENTE',
  'CAIXA',
  'ESTOQUISTA'
]

const form = reactive({
  nome: '',
  email: '',
  senha: '',
  papel: 'CAIXA',
  ativo: true
})

async function carregar() {
  usuarios.value = await usuarioApi.listar()
}

function limpar() {
  Object.assign(form, {
    nome: '',
    email: '',
    senha: '',
    papel: 'CAIXA',
    ativo: true
  })
  usuarioEmEdicaoId.value = null
}

function editar(usuario) {
  usuarioEmEdicaoId.value = usuario.id
  Object.assign(form, {
    nome: usuario.nome,
    email: usuario.email,
    senha: '',
    papel: usuario.papel,
    ativo: usuario.ativo
  })
}

async function salvar() {
  const payload = {
    nome: form.nome.trim(),
    email: form.email.trim(),
    senha: form.senha,
    papel: form.papel,
    ativo: form.ativo
  }

  try {
    if (usuarioEmEdicaoId.value) {
      await usuarioApi.atualizar(usuarioEmEdicaoId.value, payload)
      notifications.show('Usuario atualizado com sucesso.')
    } else {
      await usuarioApi.criar(payload)
      notifications.show('Usuario cadastrado com sucesso.')
    }
    limpar()
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel salvar o usuario.', 'error')
  }
}

async function excluir(usuario) {
  const confirmou = window.confirm(`Excluir o usuario "${usuario.nome}" de forma definitiva?`)
  if (!confirmou) {
    return
  }

  try {
    await usuarioApi.excluir(usuario.id)
    if (usuarioEmEdicaoId.value === usuario.id) {
      limpar()
    }
    notifications.show('Usuario excluido com sucesso.')
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel excluir o usuario.', 'error')
  }
}

function usuarioAtual(usuario) {
  return usuario.id === auth.user?.id
}

onMounted(carregar)
</script>

<template>
  <section class="grid two-columns">
    <article class="card">
      <div class="toolbar">
        <h2>Usuarios cadastrados</h2>
        <span class="badge">{{ usuarios.length }} usuarios</span>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Nome</th>
              <th>Email</th>
              <th>Perfil</th>
              <th>Status</th>
              <th>Acoes</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="usuario in usuarios" :key="usuario.id">
              <td>
                <strong>{{ usuario.nome }}</strong>
                <p v-if="usuarioAtual(usuario)">Sessao atual</p>
              </td>
              <td>{{ usuario.email }}</td>
              <td>{{ usuario.papel }}</td>
              <td>
                <span class="status-pill" :class="{ inactive: !usuario.ativo }">
                  {{ usuario.ativo ? 'Ativo' : 'Inativo' }}
                </span>
              </td>
              <td>
                <div class="table-actions">
                  <button class="icon-button" type="button" @click="editar(usuario)" aria-label="Editar usuario">
                    <Pencil :size="17" />
                  </button>
                  <button
                    class="icon-button danger-icon"
                    type="button"
                    :disabled="usuarioAtual(usuario)"
                    @click="excluir(usuario)"
                    aria-label="Excluir usuario"
                  >
                    <Trash2 :size="17" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <article class="card">
      <div class="toolbar">
        <h2>{{ usuarioEmEdicaoId ? 'Editar usuario' : 'Novo usuario' }}</h2>
        <button v-if="usuarioEmEdicaoId" class="button secondary" type="button" @click="limpar">
          <X :size="17" />
          Cancelar
        </button>
      </div>

      <form class="grid" @submit.prevent="salvar">
        <label>
          Nome
          <input v-model="form.nome" required />
        </label>
        <label>
          Email
          <input v-model="form.email" autocomplete="username" type="email" required />
        </label>
        <label>
          {{ usuarioEmEdicaoId ? 'Nova senha' : 'Senha' }}
          <input v-model="form.senha" autocomplete="new-password" type="password" :required="!usuarioEmEdicaoId" />
        </label>
        <label>
          Perfil
          <select v-model="form.papel" required>
            <option v-for="papel in papeis" :key="papel" :value="papel">{{ papel }}</option>
          </select>
        </label>
        <label class="check-row">
          <input v-model="form.ativo" type="checkbox" />
          Usuario ativo
        </label>
        <button class="button" type="submit">
          <component :is="usuarioEmEdicaoId ? Save : Plus" :size="17" />
          {{ usuarioEmEdicaoId ? 'Salvar alteracoes' : 'Cadastrar' }}
        </button>
      </form>
    </article>
  </section>
</template>
