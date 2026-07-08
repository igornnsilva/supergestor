package com.supergestor.mercado.repository;

import com.supergestor.mercado.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByNomeContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(String nome, String codigoBarras);

    @Query("select p from Produto p where p.quantidadeEstoque <= p.quantidadeMinima and p.ativo = true")
    List<Produto> buscarProdutosComEstoqueBaixo();
}

