<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Eye, RotateCcw, Plus, Trash2, WalletCards } from '@lucide/vue'
import { cadastroApi, produtoApi, vendaApi } from '../services/api'
import { useNotificationsStore } from '../stores/notifications'
import { useAuthStore } from '../stores/auth'
import VendaDetalhesModal from '../components/VendaDetalhesModal.vue'

const notifications = useNotificationsStore()
const auth = useAuthStore()
const produtos = ref([])
const clientes = ref([])
const vendas = ref([])
const itens = ref([])
const pagamentos = ref([])
const vendaSelecionada = ref(null)
let pagamentoSequence = 0

const form = reactive({
  produtoId: '',
  quantidade: 1,
  clienteId: '',
  desconto: '0'
})

const formasPagamento = [
  { value: 'PIX', label: 'PIX' },
  { value: 'DINHEIRO', label: 'Dinheiro' },
  { value: 'CARTAO_CREDITO', label: 'Cartao de credito' },
  { value: 'CARTAO_DEBITO', label: 'Cartao de debito' }
]

const money = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
})

const subtotal = computed(() =>
  itens.value.reduce((sum, item) => sum + item.produto.precoVenda * item.quantidade, 0)
)

const total = computed(() => Math.max(0, subtotal.value - parseDecimal(form.desconto)))

const totalPagamentos = computed(() =>
  pagamentos.value.reduce((sum, pagamento) => sum + parseDecimal(pagamento.valor), 0)
)

const saldoPagamento = computed(() => roundMoney(total.value - totalPagamentos.value))

const pagamentosValidos = computed(() =>
  itens.value.length > 0 &&
  pagamentos.value.length > 0 &&
  pagamentos.value.every((pagamento) => pagamento.formaPagamento && parseDecimal(pagamento.valor) > 0) &&
  new Set(pagamentos.value.map((pagamento) => pagamento.formaPagamento)).size === pagamentos.value.length &&
  Math.abs(saldoPagamento.value) < 0.005
)

function parseDecimal(value) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0
  }

  const raw = String(value ?? '').trim().replace(/\s/g, '')
  if (!raw) {
    return 0
  }

  const lastComma = raw.lastIndexOf(',')
  const lastDot = raw.lastIndexOf('.')
  let normalized = raw

  if (lastComma >= 0 && lastDot >= 0) {
    const decimalSeparator = lastComma > lastDot ? ',' : '.'
    const thousandsSeparator = decimalSeparator === ',' ? '.' : ','
    normalized = raw
      .replaceAll(thousandsSeparator, '')
      .replace(decimalSeparator, '.')
  } else {
    normalized = raw.replace(',', '.')
  }

  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

function roundMoney(value) {
  return Math.round((Number(value) + Number.EPSILON) * 100) / 100
}

function toMoneyInput(value) {
  return roundMoney(value).toFixed(2).replace('.', ',')
}

function sanitizeIntegerInput(value) {
  return String(value ?? '').replace(/\D/g, '')
}

function atualizarQuantidade(event) {
  const sanitized = sanitizeIntegerInput(event.target.value)
  form.quantidade = sanitized ? Number(sanitized) : ''
}

function bloquearQuantidadeDecimal(event) {
  if (['.', ',', 'e', 'E', '+', '-'].includes(event.key)) {
    event.preventDefault()
  }
}

function adicionarItem() {
  const produto = produtos.value.find((item) => item.id === Number(form.produtoId))
  const quantidade = Number(form.quantidade)
  if (!produto) {
    return
  }
  if (!Number.isInteger(quantidade) || quantidade <= 0) {
    notifications.show('Informe uma quantidade inteira maior que zero.', 'error')
    form.quantidade = 1
    return
  }
  const existente = itens.value.find((item) => item.produto.id === produto.id)
  if (existente) {
    existente.quantidade += quantidade
  } else {
    itens.value.push({ produto, quantidade })
  }
  form.quantidade = 1
  if (pagamentos.value.length === 1 && !pagamentos.value[0].valor) {
    completarPagamento(pagamentos.value[0])
  }
}

function removerItem(produtoId) {
  itens.value = itens.value.filter((item) => item.produto.id !== produtoId)
}

function primeiraFormaDisponivel(pagamentoAtual = null) {
  return formasPagamento.find((forma) =>
    !pagamentos.value.some((pagamento) =>
      pagamento.id !== pagamentoAtual?.id && pagamento.formaPagamento === forma.value
    )
  )?.value ?? ''
}

function formasDisponiveis(pagamentoAtual) {
  return formasPagamento.filter((forma) =>
    forma.value === pagamentoAtual.formaPagamento ||
    !pagamentos.value.some((pagamento) =>
      pagamento.id !== pagamentoAtual.id && pagamento.formaPagamento === forma.value
    )
  )
}

function criarPagamento(valor = '') {
  pagamentoSequence += 1
  return {
    id: pagamentoSequence,
    formaPagamento: primeiraFormaDisponivel(),
    valor
  }
}

function adicionarPagamento() {
  if (!primeiraFormaDisponivel()) {
    notifications.show('Todas as formas de pagamento ja foram adicionadas.', 'error')
    return
  }

  if (
    pagamentos.value.length === 1 &&
    total.value > 0 &&
    roundMoney(totalPagamentos.value) === roundMoney(total.value)
  ) {
    const primeiraParcela = roundMoney(total.value / 2)
    pagamentos.value[0].valor = toMoneyInput(primeiraParcela)
    pagamentos.value.push(criarPagamento(toMoneyInput(total.value - primeiraParcela)))
    return
  }

  pagamentos.value.push(criarPagamento(saldoPagamento.value > 0 ? toMoneyInput(saldoPagamento.value) : ''))
}

function removerPagamento(id) {
  pagamentos.value = pagamentos.value.filter((pagamento) => pagamento.id !== id)
}

function completarPagamento(pagamento) {
  const outrosPagamentos = pagamentos.value
    .filter((item) => item.id !== pagamento.id)
    .reduce((sum, item) => sum + parseDecimal(item.valor), 0)
  const restante = roundMoney(total.value - outrosPagamentos)
  pagamento.valor = restante > 0 ? toMoneyInput(restante) : ''
}

function resetarPagamentos() {
  pagamentos.value = [criarPagamento()]
}

watch(total, (novoTotal) => {
  if (pagamentos.value.length === 1 && pagamentos.value[0].valor) {
    pagamentos.value[0].valor = toMoneyInput(novoTotal)
  }
})

function descreverPagamento(venda) {
  if (venda.pagamentos?.length) {
    return venda.pagamentos
      .map((pagamento) => `${pagamento.formaPagamento}: ${money.format(pagamento.valor)}`)
      .join(' + ')
  }
  return venda.formaPagamento
}

function abrirDetalhes(venda) {
  vendaSelecionada.value = venda
}

function fecharDetalhes() {
  vendaSelecionada.value = null
}

async function estornarVenda(venda) {
  const confirmou = window.confirm(`Estornar a venda #${venda.id} e devolver os itens ao estoque?`)
  if (!confirmou) {
    return
  }

  try {
    await vendaApi.estornar(venda.id)
    notifications.show('Venda estornada e estoque devolvido.')
    await carregar()
  } catch (error) {
    notifications.show(error.response?.data?.detail ?? 'Nao foi possivel estornar a venda.', 'error')
  }
}

async function finalizarVenda() {
  if (!pagamentosValidos.value) {
    notifications.show('A soma dos pagamentos precisa ser igual ao total da venda.', 'error')
    return
  }

  const pagamentosNormalizados = pagamentos.value.map((pagamento) => ({
    formaPagamento: pagamento.formaPagamento,
    valor: roundMoney(parseDecimal(pagamento.valor))
  }))

  try {
    await vendaApi.criar({
      clienteId: form.clienteId ? Number(form.clienteId) : null,
      usuarioId: Number(auth.user.id),
      formaPagamento: pagamentosNormalizados[0].formaPagamento,
      desconto: roundMoney(parseDecimal(form.desconto)),
      pagamentos: pagamentosNormalizados,
      itens: itens.value.map((item) => ({
        produtoId: item.produto.id,
        quantidade: item.quantidade
      }))
    })
    itens.value = []
    form.desconto = '0'
    resetarPagamentos()
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
  if (!pagamentos.value.length) {
    resetarPagamentos()
  }
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
            <input
              :value="form.quantidade"
              inputmode="numeric"
              min="1"
              pattern="[0-9]*"
              type="text"
              @input="atualizarQuantidade"
              @keydown="bloquearQuantidadeDecimal"
            />
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
            Desconto
            <input v-model="form.desconto" inputmode="decimal" placeholder="0,00" type="text" />
          </label>
        </div>

        <section class="payment-box">
          <div class="toolbar compact">
            <h3>Pagamentos</h3>
            <button class="button secondary" type="button" :disabled="pagamentos.length >= formasPagamento.length" @click="adicionarPagamento">
              <Plus :size="17" />
              Adicionar pagamento
            </button>
          </div>

          <div class="payment-list">
            <div v-for="pagamento in pagamentos" :key="pagamento.id" class="payment-line">
              <label>
                Forma
                <select v-model="pagamento.formaPagamento">
                  <option v-for="forma in formasDisponiveis(pagamento)" :key="forma.value" :value="forma.value">
                    {{ forma.label }}
                  </option>
                </select>
              </label>
              <label>
                Valor
                <input v-model="pagamento.valor" inputmode="decimal" placeholder="0,00" type="text" />
              </label>
              <button class="button secondary" type="button" @click="completarPagamento(pagamento)">
                <WalletCards :size="17" />
                Restante
              </button>
              <button
                class="button danger icon-only"
                type="button"
                @click="removerPagamento(pagamento.id)"
                aria-label="Remover pagamento"
              >
                <Trash2 :size="17" />
              </button>
            </div>
            <p v-if="!pagamentos.length" class="empty-state">Adicione ao menos uma forma de pagamento.</p>
          </div>
        </section>

        <div class="totals">
          <div><span>Subtotal</span><strong>{{ money.format(subtotal) }}</strong></div>
          <div><span>Total</span><strong>{{ money.format(total) }}</strong></div>
          <div><span>Pago</span><strong>{{ money.format(totalPagamentos) }}</strong></div>
          <div :class="{ warning: Math.abs(saldoPagamento) >= 0.005 }">
            <span>{{ saldoPagamento >= 0 ? 'Falta' : 'Excedente' }}</span>
            <strong>{{ money.format(Math.abs(saldoPagamento)) }}</strong>
          </div>
        </div>

        <button class="button" type="button" :disabled="!pagamentosValidos" @click="finalizarVenda">
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
              <th>Status</th>
              <th>Detalhes</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="venda in vendas" :key="venda.id">
              <td>#{{ venda.id }}</td>
              <td>{{ descreverPagamento(venda) }}</td>
              <td>{{ venda.itens.length }}</td>
              <td>{{ money.format(venda.total) }}</td>
              <td>
                <span class="status-pill" :class="{ inactive: venda.status === 'CANCELADA' }">
                  {{ venda.status }}
                </span>
              </td>
              <td>
                <div class="table-actions">
                  <button class="icon-button" type="button" @click="abrirDetalhes(venda)" aria-label="Ver detalhes da venda">
                    <Eye :size="17" />
                  </button>
                  <button
                    class="icon-button danger-icon"
                    type="button"
                    :disabled="venda.status === 'CANCELADA'"
                    @click="estornarVenda(venda)"
                    aria-label="Estornar venda"
                  >
                    <RotateCcw :size="17" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>

  <VendaDetalhesModal v-if="vendaSelecionada" :venda="vendaSelecionada" @close="fecharDetalhes" />
</template>
