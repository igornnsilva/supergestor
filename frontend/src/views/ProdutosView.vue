<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Plus, Search } from '@lucide/vue'
import { cadastroApi, produtoApi } from '../services/api'
import { useNotificationsStore } from '../stores/notifications'

const notifications = useNotificationsStore()
const produtos = ref([])
const categorias = ref([])
const fornecedores = ref([])
const busca = ref('')

const form = reactive({
  codigoBarras: '',
  nome: '',
  marca: '',
  categoriaId: '',
  fornecedorId: '',
  precoCusto: '',
  precoVenda: '',
  unidadeMedida: 'UNIDADE',
  quantidadeEstoque: '',
  quantidadeMinima: '',
  ativo: true
})

const money = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
})

async function carregar() {
  produtos.value = await produtoApi.listar(busca.value)
}

function limpar() {
  Object.assign(form, {
    codigoBarras: '',
    nome: '',
    marca: '',
    categoriaId: categorias.value[0]?.id ?? '',
    fornecedorId: fornecedores.value[0]?.id ?? '',
    precoCusto: '',
    precoVenda: '',
    unidadeMedida: 'UNIDADE',
    quantidadeEstoque: '',
    quantidadeMinima: '',
    ativo: true
  })
}

async function salvar() {
  try {
    await produtoApi.criar({
      ...form,
      categoriaId: Number(form.categoriaId),
      fornecedorId: Number(form.fornecedorId),
      precoCusto: Number(form.precoCusto),
      precoVenda: Number(form.precoVenda),
      quantidadeEstoque: Number(form.quantidadeEstoque),
      quantidadeMinima: Number(form.quantidadeMinima)
    })
    notifications.show('Produto cadastrado com sucesso.')
    limpar()
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel salvar o produto.', 'error')
  }
}

onMounted(async () => {
  const [categoriasData, fornecedoresData] = await Promise.all([
    cadastroApi.categorias(),
    cadastroApi.fornecedores()
  ])
  categorias.value = categoriasData
  fornecedores.value = fornecedoresData
  limpar()
  await carregar()
})
</script>

<template>
  <section class="grid two-columns">
    <article class="card">
      <div class="toolbar">
        <h2>Produtos cadastrados</h2>
        <div class="form-row">
          <input v-model="busca" type="search" placeholder="Buscar por nome ou codigo" @keyup.enter="carregar" />
          <button class="button secondary" type="button" @click="carregar">
            <Search :size="17" />
            Buscar
          </button>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Codigo</th>
              <th>Produto</th>
              <th>Categoria</th>
              <th>Estoque</th>
              <th>Venda</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="produto in produtos" :key="produto.id">
              <td>{{ produto.codigoBarras }}</td>
              <td>
                <strong>{{ produto.nome }}</strong>
                <p>{{ produto.marca }}</p>
              </td>
              <td>{{ produto.categoria?.nome }}</td>
              <td>{{ produto.quantidadeEstoque }} {{ produto.unidadeMedida }}</td>
              <td>{{ money.format(produto.precoVenda) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <article class="card">
      <h2>Novo produto</h2>
      <form class="grid" @submit.prevent="salvar">
        <label>
          Codigo de barras
          <input v-model="form.codigoBarras" required />
        </label>
        <label>
          Nome
          <input v-model="form.nome" required />
        </label>
        <label>
          Marca
          <input v-model="form.marca" />
        </label>
        <div class="form-row">
          <label>
            Categoria
            <select v-model="form.categoriaId" required>
              <option v-for="categoria in categorias" :key="categoria.id" :value="categoria.id">
                {{ categoria.nome }}
              </option>
            </select>
          </label>
          <label>
            Fornecedor
            <select v-model="form.fornecedorId" required>
              <option v-for="fornecedor in fornecedores" :key="fornecedor.id" :value="fornecedor.id">
                {{ fornecedor.razaoSocial }}
              </option>
            </select>
          </label>
        </div>
        <div class="form-row">
          <label>
            Custo
            <input v-model="form.precoCusto" min="0" step="0.01" type="number" required />
          </label>
          <label>
            Venda
            <input v-model="form.precoVenda" min="0" step="0.01" type="number" required />
          </label>
        </div>
        <div class="form-row">
          <label>
            Unidade
            <select v-model="form.unidadeMedida">
              <option>UNIDADE</option>
              <option>KG</option>
              <option>LITRO</option>
              <option>PACOTE</option>
              <option>CAIXA</option>
            </select>
          </label>
          <label>
            Estoque
            <input v-model="form.quantidadeEstoque" min="0" step="0.01" type="number" required />
          </label>
          <label>
            Minimo
            <input v-model="form.quantidadeMinima" min="0" step="0.01" type="number" required />
          </label>
        </div>
        <button class="button" type="submit">
          <Plus :size="17" />
          Cadastrar
        </button>
      </form>
    </article>
  </section>
</template>
