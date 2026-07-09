package com.supergestor.mercado.repository;

import com.supergestor.mercado.model.ItemVenda;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    boolean existsByProdutoId(Long produtoId);
}
