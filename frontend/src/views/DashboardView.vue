<script setup>
import { onMounted, ref } from 'vue'
import { AlertTriangle, BadgeDollarSign, Package, ShoppingBag, Users } from '@lucide/vue'
import { dashboardApi, estoqueApi, vendaApi } from '../services/api'

const resumo = ref(null)
const estoqueBaixo = ref([])
const vendas = ref([])
const loading = ref(true)

const money = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
})

onMounted(async () => {
  const [resumoData, estoqueData, vendasData] = await Promise.all([
    dashboardApi.resumo(),
    estoqueApi.baixo(),
    vendaApi.listar()
  ])
  resumo.value = resumoData
  estoqueBaixo.value = estoqueData
  vendas.value = vendasData.slice(-5).reverse()
  loading.value = false
})
</script>

<template>
  <section v-if="!loading" class="grid">
    <div class="grid stats-grid">
      <article class="card stat-card">
        <span><BadgeDollarSign :size="17" /> Hoje</span>
        <strong>{{ money.format(resumo.faturamentoHoje) }}</strong>
      </article>
      <article class="card stat-card">
        <span><ShoppingBag :size="17" /> Vendas hoje</span>
        <strong>{{ resumo.vendasHoje }}</strong>
      </article>
      <article class="card stat-card">
        <span><Package :size="17" /> Produtos</span>
        <strong>{{ resumo.produtosCadastrados }}</strong>
      </article>
      <article class="card stat-card">
        <span><Users :size="17" /> Clientes</span>
        <strong>{{ resumo.clientesCadastrados }}</strong>
      </article>
    </div>

    <div class="grid two-columns">
      <article class="card">
        <div class="toolbar">
          <h2>Ultimas vendas</h2>
          <span class="badge">{{ money.format(resumo.faturamentoMes) }} no mes</span>
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

      <article class="card">
        <div class="toolbar">
          <h2>Estoque baixo</h2>
          <span class="badge"><AlertTriangle :size="16" /> {{ resumo.produtosEstoqueBaixo }}</span>
        </div>
        <div v-if="estoqueBaixo.length" class="sale-list">
          <div v-for="produto in estoqueBaixo" :key="produto.id" class="sale-line">
            <div>
              <strong>{{ produto.nome }}</strong>
              <p>{{ produto.categoria?.nome }} | minimo {{ produto.quantidadeMinima }}</p>
            </div>
            <strong>{{ produto.quantidadeEstoque }}</strong>
          </div>
        </div>
        <p v-else class="empty-state">Nenhum produto abaixo do minimo.</p>
      </article>
    </div>
  </section>
</template>
