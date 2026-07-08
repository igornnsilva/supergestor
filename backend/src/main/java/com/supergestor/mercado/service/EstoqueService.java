package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.EstoqueEntradaRequest;
import com.supergestor.mercado.model.MovimentacaoEstoque;
import com.supergestor.mercado.model.Produto;
import com.supergestor.mercado.model.TipoMovimentacao;
import com.supergestor.mercado.repository.MovimentacaoEstoqueRepository;
import com.supergestor.mercado.repository.ProdutoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(ProdutoRepository produtoRepository, MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<Produto> listarEstoqueBaixo() {
        return produtoRepository.buscarProdutosComEstoqueBaixo();
    }

    public List<MovimentacaoEstoque> listarMovimentacoes() {
        return movimentacaoRepository.findAll();
    }

    @Transactional
    public Produto registrarEntrada(EstoqueEntradaRequest request) {
        Produto produto = produtoRepository.findById(request.produtoId())
            .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado: " + request.produtoId()));

        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque().add(request.quantidade()));
        movimentacaoRepository.save(new MovimentacaoEstoque(
            produto,
            TipoMovimentacao.ENTRADA,
            request.quantidade(),
            request.observacao() == null ? "Entrada manual" : request.observacao()
        ));
        return produtoRepository.save(produto);
    }
}

