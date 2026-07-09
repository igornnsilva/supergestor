package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.ProdutoRequest;
import com.supergestor.mercado.model.Categoria;
import com.supergestor.mercado.model.Fornecedor;
import com.supergestor.mercado.model.Produto;
import com.supergestor.mercado.repository.CategoriaRepository;
import com.supergestor.mercado.repository.FornecedorRepository;
import com.supergestor.mercado.repository.ItemVendaRepository;
import com.supergestor.mercado.repository.MovimentacaoEstoqueRepository;
import com.supergestor.mercado.repository.ProdutoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final ItemVendaRepository itemVendaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository,
                          FornecedorRepository fornecedorRepository, ItemVendaRepository itemVendaRepository,
                          MovimentacaoEstoqueRepository movimentacaoEstoqueRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.itemVendaRepository = itemVendaRepository;
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
    }

    public List<Produto> listar(String busca) {
        if (busca == null || busca.isBlank()) {
            return produtoRepository.findAll();
        }
        return produtoRepository.findByNomeContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(busca, busca);
    }

    public Produto buscar(Long id) {
        return produtoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado: " + id));
    }

    public Produto criar(ProdutoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
            .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada: " + request.categoriaId()));
        Fornecedor fornecedor = fornecedorRepository.findById(request.fornecedorId())
            .orElseThrow(() -> new EntityNotFoundException("Fornecedor nao encontrado: " + request.fornecedorId()));

        Produto produto = new Produto(
            request.codigoBarras(),
            request.nome(),
            request.marca(),
            categoria,
            fornecedor,
            request.precoCusto(),
            request.precoVenda(),
            request.unidadeMedida(),
            request.quantidadeEstoque(),
            request.quantidadeMinima()
        );
        produto.setAtivo(request.ativo());
        return produtoRepository.save(produto);
    }

    public Produto atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscar(id);
        Categoria categoria = categoriaRepository.findById(request.categoriaId())
            .orElseThrow(() -> new EntityNotFoundException("Categoria nao encontrada: " + request.categoriaId()));
        Fornecedor fornecedor = fornecedorRepository.findById(request.fornecedorId())
            .orElseThrow(() -> new EntityNotFoundException("Fornecedor nao encontrado: " + request.fornecedorId()));

        produto.setCodigoBarras(request.codigoBarras());
        produto.setNome(request.nome());
        produto.setMarca(request.marca());
        produto.setCategoria(categoria);
        produto.setFornecedor(fornecedor);
        produto.setPrecoCusto(request.precoCusto());
        produto.setPrecoVenda(request.precoVenda());
        produto.setUnidadeMedida(request.unidadeMedida());
        produto.setQuantidadeEstoque(request.quantidadeEstoque());
        produto.setQuantidadeMinima(request.quantidadeMinima());
        produto.setAtivo(request.ativo());
        return produtoRepository.save(produto);
    }

    public void excluir(Long id) {
        Produto produto = buscar(id);
        if (itemVendaRepository.existsByProdutoId(id)) {
            throw new IllegalArgumentException("Produto possui vendas registradas e nao pode ser excluido sem quebrar o historico.");
        }
        if (movimentacaoEstoqueRepository.existsByProdutoId(id)) {
            throw new IllegalArgumentException("Produto possui movimentacoes de estoque e nao pode ser excluido sem quebrar o historico.");
        }
        produtoRepository.delete(produto);
    }
}
