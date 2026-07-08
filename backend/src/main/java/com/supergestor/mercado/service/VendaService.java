package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.VendaItemRequest;
import com.supergestor.mercado.dto.VendaRequest;
import com.supergestor.mercado.model.Cliente;
import com.supergestor.mercado.model.ItemVenda;
import com.supergestor.mercado.model.MovimentacaoEstoque;
import com.supergestor.mercado.model.Produto;
import com.supergestor.mercado.model.TipoMovimentacao;
import com.supergestor.mercado.model.Usuario;
import com.supergestor.mercado.model.Venda;
import com.supergestor.mercado.repository.ClienteRepository;
import com.supergestor.mercado.repository.MovimentacaoEstoqueRepository;
import com.supergestor.mercado.repository.ProdutoRepository;
import com.supergestor.mercado.repository.UsuarioRepository;
import com.supergestor.mercado.repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VendaService {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public VendaService(VendaRepository vendaRepository, ProdutoRepository produtoRepository,
                        ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                        MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<Venda> listar() {
        return vendaRepository.findAll();
    }

    public Venda buscar(Long id) {
        return vendaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Venda nao encontrada: " + id));
    }

    @Transactional
    public Venda finalizarVenda(VendaRequest request) {
        Cliente cliente = request.clienteId() == null ? null : clienteRepository.findById(request.clienteId())
            .orElseThrow(() -> new EntityNotFoundException("Cliente nao encontrado: " + request.clienteId()));
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
            .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado: " + request.usuarioId()));

        BigDecimal desconto = request.desconto() == null ? BigDecimal.ZERO : request.desconto();
        if (desconto.signum() < 0) {
            throw new IllegalArgumentException("Desconto nao pode ser negativo.");
        }

        Venda venda = new Venda(cliente, usuario, request.formaPagamento(), desconto);

        for (VendaItemRequest itemRequest : request.itens()) {
            Produto produto = produtoRepository.findById(itemRequest.produtoId())
                .orElseThrow(() -> new EntityNotFoundException("Produto nao encontrado: " + itemRequest.produtoId()));

            if (!produto.isAtivo()) {
                throw new IllegalArgumentException("Produto inativo nao pode ser vendido: " + produto.getNome());
            }
            if (produto.getQuantidadeEstoque().compareTo(itemRequest.quantidade()) < 0) {
                throw new IllegalArgumentException("Estoque insuficiente para " + produto.getNome());
            }

            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque().subtract(itemRequest.quantidade()));
            venda.adicionarItem(new ItemVenda(produto, itemRequest.quantidade()));
            movimentacaoRepository.save(new MovimentacaoEstoque(
                produto,
                TipoMovimentacao.SAIDA,
                itemRequest.quantidade(),
                "Saida por venda"
            ));
        }

        venda.recalcularTotais();
        Venda vendaSalva = vendaRepository.save(venda);
        if (cliente != null) {
            cliente.setPontosFidelidade(cliente.getPontosFidelidade() + vendaSalva.getTotal().intValue());
        }
        return vendaSalva;
    }
}

