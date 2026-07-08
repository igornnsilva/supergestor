package com.supergestor.mercado.controller;

import com.supergestor.mercado.dto.EstoqueEntradaRequest;
import com.supergestor.mercado.model.MovimentacaoEstoque;
import com.supergestor.mercado.model.Produto;
import com.supergestor.mercado.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping("/baixo")
    List<Produto> estoqueBaixo() {
        return estoqueService.listarEstoqueBaixo();
    }

    @GetMapping("/movimentacoes")
    List<MovimentacaoEstoque> movimentacoes() {
        return estoqueService.listarMovimentacoes();
    }

    @PostMapping("/entrada")
    Produto registrarEntrada(@Valid @RequestBody EstoqueEntradaRequest request) {
        return estoqueService.registrarEntrada(request);
    }
}

