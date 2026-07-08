package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.ResumoDashboardResponse;
import com.supergestor.mercado.repository.ClienteRepository;
import com.supergestor.mercado.repository.ProdutoRepository;
import com.supergestor.mercado.repository.VendaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DashboardService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;

    public DashboardService(VendaRepository vendaRepository, ProdutoRepository produtoRepository,
                            ClienteRepository clienteRepository) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
    }

    public ResumoDashboardResponse resumo() {
        LocalDate hoje = LocalDate.now();
        LocalDate primeiroDiaMes = hoje.withDayOfMonth(1);
        return new ResumoDashboardResponse(
            vendaRepository.somarTotalEntre(hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay()),
            vendaRepository.somarTotalEntre(primeiroDiaMes.atStartOfDay(), hoje.plusDays(1).atStartOfDay()),
            vendaRepository.countByDataVendaBetween(hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay()),
            produtoRepository.count(),
            clienteRepository.count(),
            produtoRepository.buscarProdutosComEstoqueBaixo().size()
        );
    }
}

