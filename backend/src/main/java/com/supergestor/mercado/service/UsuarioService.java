package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.UsuarioRequest;
import com.supergestor.mercado.model.PapelUsuario;
import com.supergestor.mercado.model.Usuario;
import com.supergestor.mercado.repository.UsuarioRepository;
import com.supergestor.mercado.repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final VendaRepository vendaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          VendaRepository vendaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.vendaRepository = vendaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado: " + id));
    }

    public Usuario criar(UsuarioRequest request) {
        validarSenhaObrigatoria(request.senha());
        validarEmailDisponivel(request.email(), null);

        Usuario usuario = new Usuario(
            request.nome(),
            request.email(),
            request.papel(),
            passwordEncoder.encode(request.senha())
        );
        usuario.setAtivo(request.ativo());
        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long id, UsuarioRequest request, Long usuarioLogadoId) {
        Usuario usuario = buscar(id);
        validarEmailDisponivel(request.email(), id);

        if (usuario.getId().equals(usuarioLogadoId) && !request.ativo()) {
            throw new IllegalArgumentException("Voce nao pode desativar o proprio usuario logado.");
        }
        if (usuario.getId().equals(usuarioLogadoId) && usuario.getPapel() != request.papel()) {
            throw new IllegalArgumentException("Voce nao pode alterar o proprio perfil logado.");
        }
        if (usuario.getPapel() == PapelUsuario.ADMIN && request.papel() != PapelUsuario.ADMIN && ultimoAdminAtivo(usuario)) {
            throw new IllegalArgumentException("Nao e possivel remover o perfil do ultimo administrador ativo.");
        }
        if (usuario.getPapel() == PapelUsuario.ADMIN && !request.ativo() && ultimoAdminAtivo(usuario)) {
            throw new IllegalArgumentException("Nao e possivel desativar o ultimo administrador ativo.");
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }
        return usuarioRepository.save(usuario);
    }

    public void excluir(Long id, Long usuarioLogadoId) {
        Usuario usuario = buscar(id);
        if (usuario.getId().equals(usuarioLogadoId)) {
            throw new IllegalArgumentException("Voce nao pode excluir o proprio usuario logado.");
        }
        if (usuario.getPapel() == PapelUsuario.ADMIN && ultimoAdminAtivo(usuario)) {
            throw new IllegalArgumentException("Nao e possivel excluir o ultimo administrador ativo.");
        }
        if (vendaRepository.existsByUsuarioId(id)) {
            throw new IllegalArgumentException("Usuario possui vendas vinculadas e nao pode ser excluido sem quebrar o historico.");
        }
        usuarioRepository.delete(usuario);
    }

    private void validarSenhaObrigatoria(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha e obrigatoria para cadastrar usuario.");
        }
    }

    private void validarEmailDisponivel(String email, Long idAtual) {
        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(usuario -> {
            if (idAtual == null || !usuario.getId().equals(idAtual)) {
                throw new IllegalArgumentException("Ja existe um usuario cadastrado com este email.");
            }
        });
    }

    private boolean ultimoAdminAtivo(Usuario usuario) {
        return usuario.isAtivo() && usuarioRepository.countByPapelAndAtivoTrue(PapelUsuario.ADMIN) <= 1;
    }
}
