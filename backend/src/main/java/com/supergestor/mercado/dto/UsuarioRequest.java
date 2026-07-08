package com.supergestor.mercado.dto;

import com.supergestor.mercado.model.PapelUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest(
    @NotBlank String nome,
    @Email @NotBlank String email,
    @NotBlank String senha,
    @NotNull PapelUsuario papel,
    boolean ativo
) {
}

