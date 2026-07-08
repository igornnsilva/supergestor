package com.supergestor.mercado.controller;

import com.supergestor.mercado.dto.VendaRequest;
import com.supergestor.mercado.model.Venda;
import com.supergestor.mercado.service.VendaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @GetMapping
    List<Venda> listar() {
        return vendaService.listar();
    }

    @GetMapping("/{id}")
    Venda buscar(@PathVariable Long id) {
        return vendaService.buscar(id);
    }

    @PostMapping
    Venda finalizar(@Valid @RequestBody VendaRequest request) {
        return vendaService.finalizarVenda(request);
    }
}

