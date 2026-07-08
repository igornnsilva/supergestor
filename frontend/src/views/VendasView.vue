<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus, Trash2 } from '@lucide/vue'
import { cadastroApi, produtoApi, vendaApi } from '../services/api'
import { useNotificationsStore } from '../stores/notifications'
import { useAuthStore } from '../stores/auth'

const notifications = useNotificationsStore()
const auth = useAuthStore()
const produtos = ref([])
const clientes = ref([])
const vendas = ref([])
const itens = ref([])

const form = reactive({
  produtoId: '',
  quantidade: 1,
  clienteId: '',
  formaPagamento: 'PIX',
  desconto: 0
})

const money = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
})

const subtotal = computed(() =>
  itens.value.reduce((sum, item) => sum + item.produto.precoVenda * item.quantidade, 0)
)

const total = computed(() => Math.max(0, subtotal.value - Number(form.desconto || 0)))

function adicionarItem() {
  const produto = produtos.value.find((item) => item.id === Number(form.produtoId))
  if (!produto) {
    return
  }
  const existente = itens.value.find((item) => item.produto.id === produto.id)
  if (existente) {
    existente.quantidade += Number(form.quantidade)
  } else {
    itens.value.push({ produto, quantidade: Number(form.quantidade) })
  }
  form.quantidade = 1
}

function removerItem(produtoId) {
  itens.value = itens.value.filter((item) => item.produto.id !== produtoId)
}

async function finalizarVenda() {
  try {
    await vendaApi.criar({
      clienteId: form.clienteId ? Number(form.clienteId) : null,
      usuarioId: Number(auth.user.id),
      formaPagamento: form.formaPagamento,
      desconto: Number(form.desconto || 0),
      itens: itens.value.map((item) => ({
        produtoId: item.produto.id,
        quantidade: item.quantidade
      }))
    })
    itens.value = []
    form.desconto = 0
    notifications.show('Venda finalizada e estoque atualizado.')
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel finalizar a venda.', 'error')
  }
}

async function carregar() {
  const [produtosResult, clientesResult, vendasResult] = await Promise.allSettled([
    produtoApi.listar(),
    cadastroApi.clientes(),
    vendaApi.listar()
  ])

  if (produtosResult.status === 'fulfilled') {
    produtos.value = produtosResult.value
  } else {
    notifications.show('Nao foi possivel carregar os produtos da venda.', 'error')
  }

  if (clientesResult.status === 'fulfilled') {
    clientes.value = clientesResult.value
  }

  if (vendasResult.status === 'fulfilled') {
    vendas.value = vendasResult.value.slice().reverse()
  }

  form.produtoId = produtos.value[0]?.id ?? ''
  form.clienteId = clientes.value[0]?.id ?? ''
}

onMounted(carregar)
</script>

<template>
  <section class="grid two-columns">
    <article class="card">
      <h2>Nova venda</h2>
      <div class="grid">
        <div class="form-row">
          <label>
            Produto
            <select v-model="form.produtoId">
              <option v-for="produto in produtos" :key="produto.id" :value="produto.id">
                {{ produto.nome }} - {{ money.format(produto.precoVenda) }}
              </option>
            </select>
          </label>
          <label>
            Quantidade
            <input v-model="form.quantidade" min="0.01" step="0.01" type="number" />
          </label>
          <button class="button secondary" type="button" @click="adicionarItem">
            <Plus :size="17" />
            Adicionar
          </button>
        </div>

        <div class="sale-list">
          <div v-for="item in itens" :key="item.produto.id" class="sale-line">
            <div>
              <strong>{{ item.produto.nome }}</strong>
              <p>{{ item.quantidade }} x {{ money.format(item.produto.precoVenda) }}</p>
            </div>
            <button class="button danger" type="button" @click="removerItem(item.produto.id)" aria-label="Remover item">
              <Trash2 :size="17" />
            </button>
          </div>
          <p v-if="!itens.length" class="empty-state">Adicione produtos para montar o carrinho.</p>
        </div>

        <div class="form-row">
          <label>
            Cliente
            <select v-model="form.clienteId">
              <option value="">Consumidor final</option>
              <option v-for="cliente in clientes" :key="cliente.id" :value="cliente.id">
                {{ cliente.nome }}
              </option>
            </select>
          </label>
          <label>
            Caixa
            <input :value="auth.user?.nome" disabled />
          </label>
        </div>

        <div class="form-row">
          <label>
            Pagamento
            <select v-model="form.formaPagamento">
              <option>PIX</option>
              <option>DINHEIRO</option>
              <option>CARTAO_CREDITO</option>
              <option>CARTAO_DEBITO</option>
            </select>
          </label>
          <label>
            Desconto
            <input v-model="form.desconto" min="0" step="0.01" type="number" />
          </label>
        </div>

        <div class="totals">
          <div><span>Subtotal</span><strong>{{ money.format(subtotal) }}</strong></div>
          <div><span>Total</span><strong>{{ money.format(total) }}</strong></div>
        </div>

        <button class="button" type="button" :disabled="!itens.length" @click="finalizarVenda">
          Finalizar venda
        </button>
      </div>
    </article>

    <article class="card">
      <div class="toolbar">
        <h2>Historico</h2>
        <span class="badge">{{ vendas.length }} vendas</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Pagamento</th>
              <th>Itens</th>
              <th>Total</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="venda in vendas" :key="venda.id">
              <td>#{{ venda.id }}</td>
              <td>{{ venda.formaPagamento }}</td>
              <td>{{ venda.itens.length }}</td>
              <td>{{ money.format(venda.total) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>
</template>
