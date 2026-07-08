package com.supergestor.mercado.dto;

import java.math.BigDecimal;

public record ResumoDashboardResponse(
    BigDecimal faturamentoHoje,
    BigDecimal faturamentoMes,
    long vendasHoje,
    long produtosCadastrados,
    long clientesCadastrados,
    long produtosEstoqueBaixo
) {
}

