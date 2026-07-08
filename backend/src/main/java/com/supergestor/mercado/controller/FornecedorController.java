package com.supergestor.mercado.controller;

import com.supergestor.mercado.model.Fornecedor;
import com.supergestor.mercado.repository.FornecedorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorController(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @GetMapping
    List<Fornecedor> listar() {
        return fornecedorRepository.findAll();
    }

    @PostMapping
    Fornecedor criar(@Valid @RequestBody Fornecedor fornecedor) {
        return fornecedorRepository.save(fornecedor);
    }

    @PutMapping("/{id}")
    Fornecedor atualizar(@PathVariable Long id, @Valid @RequestBody Fornecedor dados) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Fornecedor nao encontrado: " + id));
        fornecedor.setRazaoSocial(dados.getRazaoSocial());
        fornecedor.setCnpj(dados.getCnpj());
        fornecedor.setTelefone(dados.getTelefone());
        fornecedor.setEmail(dados.getEmail());
        fornecedor.setAtivo(dados.isAtivo());
        return fornecedorRepository.save(fornecedor);
    }
}

