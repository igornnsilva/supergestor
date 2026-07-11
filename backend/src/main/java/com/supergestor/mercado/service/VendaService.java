package com.supergestor.mercado.service;

import com.supergestor.mercado.dto.PagamentoVendaRequest;
import com.supergestor.mercado.dto.VendaItemRequest;
import com.supergestor.mercado.dto.VendaRequest;
import com.supergestor.mercado.model.Cliente;
import com.supergestor.mercado.model.FormaPagamento;
import com.supergestor.mercado.model.ItemVenda;
import com.supergestor.mercado.model.MovimentacaoEstoque;
import com.supergestor.mercado.model.PagamentoVenda;
import com.supergestor.mercado.model.Produto;
import com.supergestor.mercado.model.StatusVenda;
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
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    public Venda estornar(Long id) {
        Venda venda = buscar(id);
        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new IllegalArgumentException("Venda ja esta cancelada.");
        }

        venda.getItens().forEach(item -> {
            Produto produto = item.getProduto();
            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque().add(item.getQuantidade()));
            movimentacaoRepository.save(new MovimentacaoEstoque(
                produto,
                TipoMovimentacao.ENTRADA,
                item.getQuantidade(),
                "Estorno da venda #" + venda.getId()
            ));
        });

        if (venda.getCliente() != null) {
            int pontosAposEstorno = venda.getCliente().getPontosFidelidade() - venda.getTotal().intValue();
            venda.getCliente().setPontosFidelidade(Math.max(0, pontosAposEstorno));
        }

        venda.setStatus(StatusVenda.CANCELADA);
        return vendaRepository.save(venda);
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
            validarQuantidadeInteira(itemRequest.quantidade());
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
        registrarPagamentos(venda, request);
        Venda vendaSalva = vendaRepository.save(venda);
        if (cliente != null) {
            cliente.setPontosFidelidade(cliente.getPontosFidelidade() + vendaSalva.getTotal().intValue());
        }
        return vendaSalva;
    }

    private void registrarPagamentos(Venda venda, VendaRequest request) {
        BigDecimal total = normalizarMoeda(venda.getTotal());
        List<PagamentoVendaRequest> pagamentos = request.pagamentos() == null ? List.of() : request.pagamentos();

        if (total.signum() == 0) {
            venda.setFormaPagamento(request.formaPagamento());
            return;
        }

        if (pagamentos.isEmpty()) {
            if (request.formaPagamento() == null) {
                throw new IllegalArgumentException("Informe ao menos uma forma de pagamento.");
            }
            venda.setFormaPagamento(request.formaPagamento());
            venda.adicionarPagamento(new PagamentoVenda(request.formaPagamento(), total));
            return;
        }

        BigDecimal somaPagamentos = BigDecimal.ZERO;
        Set<FormaPagamento> formasUsadas = EnumSet.noneOf(FormaPagamento.class);
        for (var pagamentoRequest : pagamentos) {
            if (pagamentoRequest.formaPagamento() == FormaPagamento.MISTO) {
                throw new IllegalArgumentException("MISTO nao e uma forma de pagamento direta.");
            }
            if (!formasUsadas.add(pagamentoRequest.formaPagamento())) {
                throw new IllegalArgumentException("Nao repita a mesma forma de pagamento na venda.");
            }
            BigDecimal valor = normalizarMoeda(pagamentoRequest.valor());
            if (valor.signum() <= 0) {
                throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero.");
            }
            somaPagamentos = somaPagamentos.add(valor);
            venda.adicionarPagamento(new PagamentoVenda(pagamentoRequest.formaPagamento(), valor));
        }

        if (somaPagamentos.compareTo(total) != 0) {
            throw new IllegalArgumentException("A soma dos pagamentos deve ser igual ao total da venda.");
        }

        venda.setFormaPagamento(pagamentos.size() == 1 ? pagamentos.get(0).formaPagamento() : FormaPagamento.MISTO);
    }

    private BigDecimal normalizarMoeda(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarQuantidadeInteira(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0 || quantidade.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Quantidade da venda deve ser um numero inteiro maior que zero.");
        }
    }
}
