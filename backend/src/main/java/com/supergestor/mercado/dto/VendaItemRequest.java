package com.supergestor.mercado.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record VendaItemRequest(
    @NotNull Long produtoId,
    @NotNull @DecimalMin("1") BigDecimal quantidade
) {
}
