package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.LoginRequest;
import com.supergestor.mercado.dto.LoginResponse;
import com.supergestor.mercado.model.Usuario;
import com.supergestor.mercado.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UsuarioRepository usuarioRepository;
    private final long expirationMinutes;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtEncoder jwtEncoder,
                       UsuarioRepository usuarioRepository,
                       @Value("${app.security.jwt.expiration-minutes}") long expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.usuarioRepository = usuarioRepository;
        this.expirationMinutes = expirationMinutes;
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));

        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("supergestor")
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(expirationMinutes * 60))
            .subject(usuario.getEmail())
            .claim("id", usuario.getId())
            .claim("nome", usuario.getNome())
            .claim("papel", usuario.getPapel().name())
            .claim("scope", "ROLE_" + usuario.getPapel().name())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel());
    }
}
