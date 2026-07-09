package com.supergestor.mercado.dto;

import com.supergestor.mercado.model.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoVendaRequest(
    @NotNull FormaPagamento formaPagamento,
    @NotNull @DecimalMin("0.01") BigDecimal valor
) {
}
