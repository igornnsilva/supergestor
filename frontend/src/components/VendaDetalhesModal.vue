<script setup>
import { X } from '@lucide/vue'

const props = defineProps({
  venda: {
    type: Object,
    required: true
  }
})

defineEmits(['close'])

const money = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
})

const dateTime = new Intl.DateTimeFormat('pt-BR', {
  dateStyle: 'short',
  timeStyle: 'short'
})

function formatarData(value) {
  if (!value) {
    return 'Sem data'
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : dateTime.format(date)
}

function pagamentosDaVenda() {
  if (props.venda.pagamentos?.length) {
    return props.venda.pagamentos
  }
  return [{
    formaPagamento: props.venda.formaPagamento,
    valor: props.venda.total
  }]
}
</script>

<template>
  <div class="modal-backdrop" role="presentation" @click.self="$emit('close')">
    <section class="modal-panel" role="dialog" aria-modal="true" :aria-labelledby="`venda-${venda.id}-titulo`">
      <div class="modal-header">
        <div>
          <span class="eyebrow">Detalhes da compra</span>
          <h2 :id="`venda-${venda.id}-titulo`">Venda #{{ venda.id }}</h2>
          <p>{{ formatarData(venda.dataVenda) }}</p>
        </div>
        <button class="icon-button" type="button" @click="$emit('close')" aria-label="Fechar detalhes da venda">
          <X :size="18" />
        </button>
      </div>

      <div class="detail-grid">
        <div>
          <span>Cliente</span>
          <strong>{{ venda.cliente?.nome ?? 'Consumidor final' }}</strong>
        </div>
        <div>
          <span>Caixa</span>
          <strong>{{ venda.usuario?.nome ?? 'Nao informado' }}</strong>
        </div>
        <div>
          <span>Status</span>
          <strong>{{ venda.status }}</strong>
        </div>
      </div>

      <div class="detail-section">
        <h3>Itens vendidos</h3>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Produto</th>
                <th>Qtd.</th>
                <th>Unitario</th>
                <th>Subtotal</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in venda.itens" :key="item.id">
                <td>
                  <strong>{{ item.produto?.nome ?? 'Produto removido' }}</strong>
                  <p>{{ item.produto?.codigoBarras }}</p>
                </td>
                <td>{{ item.quantidade }}</td>
                <td>{{ money.format(item.precoUnitario) }}</td>
                <td>{{ money.format(item.subtotal) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="detail-section">
        <h3>Pagamentos</h3>
        <div class="payment-summary">
          <div v-for="pagamento in pagamentosDaVenda()" :key="`${pagamento.formaPagamento}-${pagamento.valor}`">
            <span>{{ pagamento.formaPagamento }}</span>
            <strong>{{ money.format(pagamento.valor) }}</strong>
          </div>
        </div>
      </div>

      <div class="totals modal-totals">
        <div><span>Subtotal</span><strong>{{ money.format(venda.subtotal) }}</strong></div>
        <div><span>Desconto</span><strong>{{ money.format(venda.desconto) }}</strong></div>
        <div><span>Total</span><strong>{{ money.format(venda.total) }}</strong></div>
      </div>
    </section>
  </div>
</template>
