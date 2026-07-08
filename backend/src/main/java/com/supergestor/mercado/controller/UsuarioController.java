package com.supergestor.mercado.controller;

import com.supergestor.mercado.dto.UsuarioRequest;
import com.supergestor.mercado.model.Usuario;
import com.supergestor.mercado.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @PostMapping
    Usuario criar(@Valid @RequestBody UsuarioRequest request) {
        Usuario usuario = new Usuario(
            request.nome(),
            request.email(),
            request.papel(),
            passwordEncoder.encode(request.senha())
        );
        usuario.setAtivo(request.ativo());
        return usuarioRepository.save(usuario);
    }
}
