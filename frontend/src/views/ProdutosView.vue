<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, Save, Search, Trash2, X } from '@lucide/vue'
import { cadastroApi, produtoApi } from '../services/api'
import { useNotificationsStore } from '../stores/notifications'

const notifications = useNotificationsStore()
const produtos = ref([])
const categorias = ref([])
const fornecedores = ref([])
const busca = ref('')
const produtoEmEdicaoId = ref(null)

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
  produtoEmEdicaoId.value = null
}

async function salvar() {
  const payload = {
    ...form,
    categoriaId: Number(form.categoriaId),
    fornecedorId: Number(form.fornecedorId),
    precoCusto: Number(form.precoCusto),
    precoVenda: Number(form.precoVenda),
    quantidadeEstoque: Number(form.quantidadeEstoque),
    quantidadeMinima: Number(form.quantidadeMinima)
  }

  try {
    if (produtoEmEdicaoId.value) {
      await produtoApi.atualizar(produtoEmEdicaoId.value, payload)
      notifications.show('Produto atualizado com sucesso.')
    } else {
      await produtoApi.criar(payload)
      notifications.show('Produto cadastrado com sucesso.')
    }
    limpar()
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel salvar o produto.', 'error')
  }
}

function editar(produto) {
  produtoEmEdicaoId.value = produto.id
  Object.assign(form, {
    codigoBarras: produto.codigoBarras,
    nome: produto.nome,
    marca: produto.marca ?? '',
    categoriaId: produto.categoria?.id ?? '',
    fornecedorId: produto.fornecedor?.id ?? '',
    precoCusto: produto.precoCusto,
    precoVenda: produto.precoVenda,
    unidadeMedida: produto.unidadeMedida,
    quantidadeEstoque: produto.quantidadeEstoque,
    quantidadeMinima: produto.quantidadeMinima,
    ativo: produto.ativo
  })
}

async function excluir(produto) {
  const confirmou = window.confirm(`Excluir o produto "${produto.nome}" de forma definitiva?`)
  if (!confirmou) {
    return
  }

  try {
    await produtoApi.excluir(produto.id)
    if (produtoEmEdicaoId.value === produto.id) {
      limpar()
    }
    notifications.show('Produto excluido com sucesso.')
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel excluir o produto.', 'error')
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
              <th>Acoes</th>
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
              <td>
                <div class="table-actions">
                  <button class="icon-button" type="button" @click="editar(produto)" aria-label="Editar produto">
                    <Pencil :size="17" />
                  </button>
                  <button class="icon-button danger-icon" type="button" @click="excluir(produto)" aria-label="Excluir produto">
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
        <h2>{{ produtoEmEdicaoId ? 'Editar produto' : 'Novo produto' }}</h2>
        <button v-if="produtoEmEdicaoId" class="button secondary" type="button" @click="limpar">
          <X :size="17" />
          Cancelar
        </button>
      </div>
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
        <label class="check-row">
          <input v-model="form.ativo" type="checkbox" />
          Produto ativo
        </label>
        <button class="button" type="submit">
          <component :is="produtoEmEdicaoId ? Save : Plus" :size="17" />
          {{ produtoEmEdicaoId ? 'Salvar alteracoes' : 'Cadastrar' }}
        </button>
      </form>
    </article>
  </section>
</template>
