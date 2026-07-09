package com.supergestor.mercado.repository;

import com.supergestor.mercado.model.Venda;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Override
    @EntityGraph(attributePaths = {
        "cliente",
        "usuario",
        "pagamentos",
        "itens",
        "itens.produto",
        "itens.produto.categoria",
        "itens.produto.fornecedor"
    })
    List<Venda> findAll();

    @Override
    @EntityGraph(attributePaths = {
        "cliente",
        "usuario",
        "pagamentos",
        "itens",
        "itens.produto",
        "itens.produto.categoria",
        "itens.produto.fornecedor"
    })
    Optional<Venda> findById(Long id);

    long countByDataVendaBetween(LocalDateTime inicio, LocalDateTime fim);

    @Query("select coalesce(sum(v.total), 0) from Venda v where v.dataVenda between :inicio and :fim")
    BigDecimal somarTotalEntre(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
