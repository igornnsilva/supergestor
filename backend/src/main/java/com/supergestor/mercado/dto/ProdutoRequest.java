package com.supergestor.mercado.dto;

import com.supergestor.mercado.model.UnidadeMedida;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProdutoRequest(
    @NotBlank String codigoBarras,
    @NotBlank String nome,
    String marca,
    @NotNull Long categoriaId,
    @NotNull Long fornecedorId,
    @NotNull @DecimalMin("0.00") BigDecimal precoCusto,
    @NotNull @DecimalMin("0.00") BigDecimal precoVenda,
    @NotNull UnidadeMedida unidadeMedida,
    @NotNull @DecimalMin("0.00") BigDecimal quantidadeEstoque,
    @NotNull @DecimalMin("0.00") BigDecimal quantidadeMinima,
    boolean ativo
) {
}

