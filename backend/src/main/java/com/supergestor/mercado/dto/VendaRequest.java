package com.supergestor.mercado.dto;

import com.supergestor.mercado.model.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record VendaRequest(
    Long clienteId,
    @NotNull Long usuarioId,
    FormaPagamento formaPagamento,
    BigDecimal desconto,
    @NotEmpty List<@Valid VendaItemRequest> itens,
    List<@Valid PagamentoVendaRequest> pagamentos
) {
    public VendaRequest(Long clienteId,
                        Long usuarioId,
                        FormaPagamento formaPagamento,
                        BigDecimal desconto,
                        List<VendaItemRequest> itens) {
        this(clienteId, usuarioId, formaPagamento, desconto, itens, List.of());
    }
}
