package com.supergestor.mercado.config;

import com.supergestor.mercado.model.Categoria;
import com.supergestor.mercado.model.Cliente;
import com.supergestor.mercado.model.FormaPagamento;
import com.supergestor.mercado.model.Fornecedor;
import com.supergestor.mercado.model.PapelUsuario;
import com.supergestor.mercado.model.Produto;
import com.supergestor.mercado.model.UnidadeMedida;
import com.supergestor.mercado.model.Usuario;
import com.supergestor.mercado.repository.CategoriaRepository;
import com.supergestor.mercado.repository.ClienteRepository;
import com.supergestor.mercado.repository.FornecedorRepository;
import com.supergestor.mercado.repository.ProdutoRepository;
import com.supergestor.mercado.repository.UsuarioRepository;
import com.supergestor.mercado.service.VendaService;
import com.supergestor.mercado.dto.VendaItemRequest;
import com.supergestor.mercado.dto.VendaRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(CategoriaRepository categoriaRepository,
                               FornecedorRepository fornecedorRepository,
                               ClienteRepository clienteRepository,
                               UsuarioRepository usuarioRepository,
                               ProdutoRepository produtoRepository,
                               VendaService vendaService,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            usuarioRepository.findByEmailIgnoreCase("admin@supergestor.local")
                .orElseGet(() -> usuarioRepository.save(new Usuario("Administrador", "admin@supergestor.local", PapelUsuario.ADMIN, passwordEncoder.encode("123456"))));
            Usuario caixa = usuarioRepository.findByEmailIgnoreCase("caixa@supergestor.local")
                .orElseGet(() -> usuarioRepository.save(new Usuario("Ana Caixa", "caixa@supergestor.local", PapelUsuario.CAIXA, passwordEncoder.encode("123456"))));
            usuarioRepository.findByEmailIgnoreCase("gerente@supergestor.local")
                .orElseGet(() -> usuarioRepository.save(new Usuario("Marcos Gerente", "gerente@supergestor.local", PapelUsuario.GERENTE, passwordEncoder.encode("123456"))));
            usuarioRepository.findByEmailIgnoreCase("estoque@supergestor.local")
                .orElseGet(() -> usuarioRepository.save(new Usuario("Julia Estoque", "estoque@supergestor.local", PapelUsuario.ESTOQUISTA, passwordEncoder.encode("123456"))));

            if (produtoRepository.count() > 0) {
                return;
            }

            Categoria mercearia = categoriaRepository.save(new Categoria("Mercearia", "Alimentos nao pereciveis"));
            Categoria bebidas = categoriaRepository.save(new Categoria("Bebidas", "Sucos, refrigerantes e agua"));
            Categoria limpeza = categoriaRepository.save(new Categoria("Limpeza", "Produtos de higiene e limpeza"));
            Categoria hortifruti = categoriaRepository.save(new Categoria("Hortifruti", "Frutas, legumes e verduras"));

            Fornecedor central = fornecedorRepository.save(new Fornecedor(
                "Central Distribuidora",
                "12.345.678/0001-90",
                "(11) 4002-8922",
                "compras@central.example"
            ));
            Fornecedor campo = fornecedorRepository.save(new Fornecedor(
                "Campo Bom Alimentos",
                "98.765.432/0001-10",
                "(11) 3333-1919",
                "vendas@campobom.example"
            ));

            Cliente cliente = clienteRepository.save(new Cliente("Cliente Padrao", "123.456.789-00", "(11) 99999-0000"));

            Produto arroz = produtoRepository.save(new Produto(
                "7891000000011",
                "Arroz Tipo 1 5kg",
                "Casa Boa",
                mercearia,
                central,
                new BigDecimal("19.90"),
                new BigDecimal("27.90"),
                UnidadeMedida.PACOTE,
                new BigDecimal("40"),
                new BigDecimal("12")
            ));
            Produto cafe = produtoRepository.save(new Produto(
                "7891000000028",
                "Cafe Torrado 500g",
                "Montanha",
                mercearia,
                campo,
                new BigDecimal("10.50"),
                new BigDecimal("16.90"),
                UnidadeMedida.PACOTE,
                new BigDecimal("8"),
                new BigDecimal("10")
            ));
            produtoRepository.save(new Produto(
                "7891000000035",
                "Detergente Neutro 500ml",
                "Brilha Mais",
                limpeza,
                central,
                new BigDecimal("1.90"),
                new BigDecimal("3.49"),
                UnidadeMedida.UNIDADE,
                new BigDecimal("80"),
                new BigDecimal("20")
            ));
            produtoRepository.save(new Produto(
                "7891000000042",
                "Suco de Uva Integral 1L",
                "Vale Verde",
                bebidas,
                campo,
                new BigDecimal("8.20"),
                new BigDecimal("13.90"),
                UnidadeMedida.LITRO,
                new BigDecimal("18"),
                new BigDecimal("8")
            ));
            produtoRepository.save(new Produto(
                "7891000000059",
                "Banana Prata",
                "Hortifruti Local",
                hortifruti,
                campo,
                new BigDecimal("3.10"),
                new BigDecimal("6.49"),
                UnidadeMedida.KG,
                new BigDecimal("25"),
                new BigDecimal("6")
            ));

            vendaService.finalizarVenda(new VendaRequest(
                cliente.getId(),
                caixa.getId(),
                FormaPagamento.PIX,
                BigDecimal.ZERO,
                List.of(
                    new VendaItemRequest(arroz.getId(), new BigDecimal("1")),
                    new VendaItemRequest(cafe.getId(), new BigDecimal("2"))
                )
            ));
        };
    }
}
