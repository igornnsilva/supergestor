package com.supergestor.mercado.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EstoqueEntradaRequest(
    @NotNull Long produtoId,
    @NotNull @DecimalMin("0.01") BigDecimal quantidade,
    String observacao
) {
}

