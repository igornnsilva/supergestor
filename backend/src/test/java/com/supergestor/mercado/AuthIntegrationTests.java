package com.supergestor.mercado;

import com.supergestor.mercado.dto.LoginRequest;
import com.supergestor.mercado.dto.LoginResponse;
import com.supergestor.mercado.dto.PagamentoVendaRequest;
import com.supergestor.mercado.dto.VendaItemRequest;
import com.supergestor.mercado.dto.VendaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supergestor.mercado.model.FormaPagamento;
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

import com.supergestor.mercado.repository.ProdutoRepository;
import com.supergestor.mercado.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
    private ProdutoRepository produtoRepository;

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

        assertThat(response.getStatusCode())
            .as("body: " + response.getBody())
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("itens");
    }

    @Test
    void vendaAceitaPagamentosMisturadosQuandoSomaFecha() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var produto = produtoRepository.findAll().get(0);
        var usuario = usuarioRepository.findByEmailIgnoreCase("admin@supergestor.local").orElseThrow();
        BigDecimal total = produto.getPrecoVenda();
        BigDecimal primeiraParcela = total.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/vendas",
            new HttpEntity<>(
                new VendaRequest(
                    null,
                    usuario.getId(),
                    FormaPagamento.MISTO,
                    BigDecimal.ZERO,
                    List.of(new VendaItemRequest(produto.getId(), BigDecimal.ONE)),
                    List.of(
                        new PagamentoVendaRequest(FormaPagamento.DINHEIRO, primeiraParcela),
                        new PagamentoVendaRequest(FormaPagamento.PIX, total.subtract(primeiraParcela))
                    )
                ),
                headers
            ),
            String.class
        );

        assertThat(response.getStatusCode())
            .as("body: " + response.getBody())
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"formaPagamento\":\"MISTO\"");
        assertThat(response.getBody()).contains("\"pagamentos\"");
        assertThat(response.getBody()).contains("\"formaPagamento\":\"DINHEIRO\"");
        assertThat(response.getBody()).contains("\"formaPagamento\":\"PIX\"");
    }

    @Test
    void vendaRejeitaPagamentosQuandoSomaNaoFecha() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var produto = produtoRepository.findAll().get(0);
        var usuario = usuarioRepository.findByEmailIgnoreCase("admin@supergestor.local").orElseThrow();

        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/vendas",
            new HttpEntity<>(
                new VendaRequest(
                    null,
                    usuario.getId(),
                    FormaPagamento.PIX,
                    BigDecimal.ZERO,
                    List.of(new VendaItemRequest(produto.getId(), BigDecimal.ONE)),
                    List.of(new PagamentoVendaRequest(FormaPagamento.PIX, BigDecimal.ONE))
                ),
                headers
            ),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("A soma dos pagamentos deve ser igual ao total da venda.");
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
