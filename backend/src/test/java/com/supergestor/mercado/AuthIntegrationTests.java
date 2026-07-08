package com.supergestor.mercado;

import com.supergestor.mercado.dto.LoginRequest;
import com.supergestor.mercado.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.supergestor.mercado.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticationManagerAceitaUsuarioSeedado() {
        var usuario = usuarioRepository.findByEmailIgnoreCase("admin@supergestor.local").orElseThrow();

        assertThat(passwordEncoder.matches("123456", usuario.getSenhaHash())).isTrue();

        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken("admin@supergestor.local", "123456")
        );

        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void loginRetornaTokenJwt() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            new LoginRequest("admin@supergestor.local", "123456"),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("body: " + response.getBody())
            .isEqualTo(HttpStatus.OK);
        LoginResponse body = objectMapper.readValue(response.getBody(), LoginResponse.class);
        assertThat(body.token()).isNotBlank();
        assertThat(body.email()).isEqualTo("admin@supergestor.local");
    }

    @Test
    void dashboardExigeAutenticacao() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/dashboard/resumo",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void vendasListaComToken() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/vendas",
            org.springframework.http.HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("itens");
    }

    private LoginResponse loginAdmin() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            new LoginRequest("admin@supergestor.local", "123456"),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readValue(response.getBody(), LoginResponse.class);
    }
}
