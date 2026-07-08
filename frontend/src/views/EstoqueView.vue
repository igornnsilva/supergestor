<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@lucide/vue'
import { estoqueApi, produtoApi } from '../services/api'
import { useNotificationsStore } from '../stores/notifications'

const notifications = useNotificationsStore()
const produtos = ref([])
const baixo = ref([])
const movimentacoes = ref([])

const form = reactive({
  produtoId: '',
  quantidade: '',
  observacao: 'Reposicao de mercadoria'
})

async function carregar() {
  const [produtosData, baixoData, movimentacoesData] = await Promise.all([
    produtoApi.listar(),
    estoqueApi.baixo(),
    estoqueApi.movimentacoes()
  ])
  produtos.value = produtosData
  baixo.value = baixoData
  movimentacoes.value = movimentacoesData.slice(-8).reverse()
  form.produtoId = produtos.value[0]?.id ?? ''
}

async function registrarEntrada() {
  try {
    await estoqueApi.entrada({
      produtoId: Number(form.produtoId),
      quantidade: Number(form.quantidade),
      observacao: form.observacao
    })
    form.quantidade = ''
    notifications.show('Entrada de estoque registrada.')
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel registrar a entrada.', 'error')
  }
}

onMounted(carregar)
</script>

<template>
  <section class="grid two-columns">
    <article class="card">
      <div class="toolbar">
        <h2>Produtos em alerta</h2>
        <span class="badge">{{ baixo.length }} itens</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Produto</th>
              <th>Atual</th>
              <th>Minimo</th>
              <th>Fornecedor</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="produto in baixo" :key="produto.id">
              <td>{{ produto.nome }}</td>
              <td>{{ produto.quantidadeEstoque }}</td>
              <td>{{ produto.quantidadeMinima }}</td>
              <td>{{ produto.fornecedor?.razaoSocial }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!baixo.length" class="empty-state">Todos os produtos estao acima do minimo.</p>
    </article>

    <article class="card">
      <h2>Entrada de estoque</h2>
      <form class="grid" @submit.prevent="registrarEntrada">
        <label>
          Produto
          <select v-model="form.produtoId" required>
            <option v-for="produto in produtos" :key="produto.id" :value="produto.id">
              {{ produto.nome }}
            </option>
          </select>
        </label>
        <label>
          Quantidade
          <input v-model="form.quantidade" min="0.01" step="0.01" type="number" required />
        </label>
        <label>
          Observacao
          <input v-model="form.observacao" />
        </label>
        <button class="button" type="submit">
          <Plus :size="17" />
          Registrar
        </button>
      </form>

      <h3>Movimentacoes recentes</h3>
      <div class="sale-list">
        <div v-for="mov in movimentacoes" :key="mov.id" class="sale-line">
          <div>
            <strong>{{ mov.produto?.nome }}</strong>
            <p>{{ mov.tipo }} | {{ mov.observacao }}</p>
          </div>
          <strong>{{ mov.quantidade }}</strong>
        </div>
      </div>
    </article>
  </section>
</template>
