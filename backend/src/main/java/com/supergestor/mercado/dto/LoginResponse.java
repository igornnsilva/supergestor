package com.supergestor.mercado.dto;

import com.supergestor.mercado.model.PapelUsuario;

public record LoginResponse(
    String token,
    Long id,
    String nome,
    String email,
    PapelUsuario papel
) {
}

