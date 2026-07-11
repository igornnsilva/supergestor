package com.supergestor.mercado;

import com.supergestor.mercado.dto.LoginRequest;
import com.supergestor.mercado.dto.LoginResponse;
import com.supergestor.mercado.dto.PagamentoVendaRequest;
import com.supergestor.mercado.dto.ProdutoRequest;
import com.supergestor.mercado.dto.UsuarioRequest;
import com.supergestor.mercado.dto.VendaItemRequest;
import com.supergestor.mercado.dto.VendaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supergestor.mercado.model.FormaPagamento;
import com.supergestor.mercado.model.PapelUsuario;
import com.supergestor.mercado.model.UnidadeMedida;
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
import com.supergestor.mercado.repository.CategoriaRepository;
import com.supergestor.mercado.repository.FornecedorRepository;
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
    private CategoriaRepository categoriaRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

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
                    FormaPagamento.DINHEIRO,
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
    void vendaRejeitaQuantidadeDecimal() throws Exception {
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
                    List.of(new VendaItemRequest(produto.getId(), new BigDecimal("1.5"))),
                    List.of(new PagamentoVendaRequest(FormaPagamento.PIX, produto.getPrecoVenda()))
                ),
                headers
            ),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Quantidade da venda deve ser um numero inteiro maior que zero.");
    }

    @Test
    void estornoCancelaVendaEDevolveEstoque() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var produto = produtoRepository.findAll().get(0);
        var usuario = usuarioRepository.findByEmailIgnoreCase("admin@supergestor.local").orElseThrow();
        BigDecimal estoqueAntes = produto.getQuantidadeEstoque();
        BigDecimal quantidadeVendida = BigDecimal.ONE;

        ResponseEntity<String> vendaCriada = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/vendas",
            new HttpEntity<>(
                new VendaRequest(
                    null,
                    usuario.getId(),
                    FormaPagamento.PIX,
                    BigDecimal.ZERO,
                    List.of(new VendaItemRequest(produto.getId(), quantidadeVendida)),
                    List.of(new PagamentoVendaRequest(FormaPagamento.PIX, produto.getPrecoVenda()))
                ),
                headers
            ),
            String.class
        );

        assertThat(vendaCriada.getStatusCode())
            .as("body: " + vendaCriada.getBody())
            .isEqualTo(HttpStatus.OK);
        Long vendaId = objectMapper.readTree(vendaCriada.getBody()).get("id").asLong();
        assertThat(produtoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque())
            .isEqualByComparingTo(estoqueAntes.subtract(quantidadeVendida));

        ResponseEntity<String> estorno = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/vendas/" + vendaId + "/estorno",
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(estorno.getStatusCode())
            .as("body: " + estorno.getBody())
            .isEqualTo(HttpStatus.OK);
        assertThat(estorno.getBody()).contains("\"status\":\"CANCELADA\"");
        assertThat(produtoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque())
            .isEqualByComparingTo(estoqueAntes);

        ResponseEntity<String> segundoEstorno = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/vendas/" + vendaId + "/estorno",
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(segundoEstorno.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(segundoEstorno.getBody()).contains("Venda ja esta cancelada.");
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

    @Test
    void produtoPodeSerEditadoEExcluidoQuandoNaoTemHistorico() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var categoria = categoriaRepository.findAll().get(0);
        var fornecedor = fornecedorRepository.findAll().get(0);

        ProdutoRequest criacao = new ProdutoRequest(
            "9000000000001",
            "Produto Temporario",
            "Teste",
            categoria.getId(),
            fornecedor.getId(),
            new BigDecimal("2.50"),
            new BigDecimal("5.00"),
            UnidadeMedida.UNIDADE,
            new BigDecimal("4"),
            new BigDecimal("1"),
            true
        );

        ResponseEntity<String> criado = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/produtos",
            new HttpEntity<>(criacao, headers),
            String.class
        );

        assertThat(criado.getStatusCode())
            .as("body: " + criado.getBody())
            .isEqualTo(HttpStatus.OK);
        Long produtoId = objectMapper.readTree(criado.getBody()).get("id").asLong();

        ProdutoRequest edicao = new ProdutoRequest(
            "9000000000001",
            "Produto Temporario Editado",
            "Teste",
            categoria.getId(),
            fornecedor.getId(),
            new BigDecimal("3.00"),
            new BigDecimal("6.75"),
            UnidadeMedida.UNIDADE,
            new BigDecimal("8"),
            new BigDecimal("2"),
            true
        );

        ResponseEntity<String> atualizado = restTemplate.exchange(
            "http://localhost:" + port + "/api/produtos/" + produtoId,
            org.springframework.http.HttpMethod.PUT,
            new HttpEntity<>(edicao, headers),
            String.class
        );

        assertThat(atualizado.getStatusCode())
            .as("body: " + atualizado.getBody())
            .isEqualTo(HttpStatus.OK);
        assertThat(produtoRepository.findById(produtoId).orElseThrow().getNome())
            .isEqualTo("Produto Temporario Editado");

        ResponseEntity<String> excluido = restTemplate.exchange(
            "http://localhost:" + port + "/api/produtos/" + produtoId,
            org.springframework.http.HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(excluido.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(produtoRepository.existsById(produtoId)).isFalse();
    }

    @Test
    void produtoComVendaNaoPodeSerExcluidoParaPreservarHistorico() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var produtoVendido = produtoRepository.findAll().stream()
            .filter(produto -> produto.getCodigoBarras().equals("7891000000011"))
            .findFirst()
            .orElseThrow();

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/produtos/" + produtoVendido.getId(),
            org.springframework.http.HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Produto possui vendas registradas");
        assertThat(produtoRepository.existsById(produtoVendido.getId())).isTrue();
    }

    @Test
    void usuarioPodeSerCriadoEditadoAutenticarComNovaSenhaEExcluido() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> criado = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/usuarios",
            new HttpEntity<>(
                new UsuarioRequest("Operador Temporario", "temp@supergestor.local", "123456", PapelUsuario.CAIXA, true),
                headers
            ),
            String.class
        );

        assertThat(criado.getStatusCode())
            .as("body: " + criado.getBody())
            .isEqualTo(HttpStatus.OK);
        Long usuarioId = objectMapper.readTree(criado.getBody()).get("id").asLong();

        ResponseEntity<String> atualizado = restTemplate.exchange(
            "http://localhost:" + port + "/api/usuarios/" + usuarioId,
            org.springframework.http.HttpMethod.PUT,
            new HttpEntity<>(
                new UsuarioRequest("Operador Editado", "temp.editado@supergestor.local", "654321", PapelUsuario.ESTOQUISTA, true),
                headers
            ),
            String.class
        );

        assertThat(atualizado.getStatusCode())
            .as("body: " + atualizado.getBody())
            .isEqualTo(HttpStatus.OK);

        ResponseEntity<String> loginEditado = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login",
            new LoginRequest("temp.editado@supergestor.local", "654321"),
            String.class
        );

        assertThat(loginEditado.getStatusCode())
            .as("body: " + loginEditado.getBody())
            .isEqualTo(HttpStatus.OK);

        ResponseEntity<String> excluido = restTemplate.exchange(
            "http://localhost:" + port + "/api/usuarios/" + usuarioId,
            org.springframework.http.HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(excluido.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarioRepository.existsById(usuarioId)).isFalse();
    }

    @Test
    void usuarioComVendaNaoPodeSerExcluidoParaPreservarHistorico() throws Exception {
        String token = loginAdmin().token();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var caixa = usuarioRepository.findByEmailIgnoreCase("caixa@supergestor.local").orElseThrow();

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/usuarios/" + caixa.getId(),
            org.springframework.http.HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Usuario possui vendas vinculadas");
        assertThat(usuarioRepository.existsById(caixa.getId())).isTrue();
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
