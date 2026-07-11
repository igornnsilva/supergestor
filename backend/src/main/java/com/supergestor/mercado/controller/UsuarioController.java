package com.supergestor.mercado.controller;

import com.supergestor.mercado.dto.UsuarioRequest;
import com.supergestor.mercado.model.Usuario;
import com.supergestor.mercado.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    List<Usuario> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    Usuario criar(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.criar(request);
    }

    @PutMapping("/{id}")
    Usuario atualizar(@PathVariable Long id,
                      @Valid @RequestBody UsuarioRequest request,
                      @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.atualizar(id, request, usuarioLogadoId(jwt));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.excluir(id, usuarioLogadoId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long usuarioLogadoId(Jwt jwt) {
        return ((Number) jwt.getClaim("id")).longValue();
    }
}
