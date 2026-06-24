package dev.adastratech.mercuryshop.cart.application;

import dev.adastratech.mercuryshop.cart.domain.Cart;
import dev.adastratech.mercuryshop.cart.domain.CartItem;
import dev.adastratech.mercuryshop.cart.domain.CartRepository;
import dev.adastratech.mercuryshop.product.domain.Product;
import dev.adastratech.mercuryshop.product.domain.ProductRepository;
import dev.adastratech.mercuryshop.shared.exception.ConflictException;
import dev.adastratech.mercuryshop.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso do carrinho. Itens guardam só produto+quantidade; preços/totais são resolvidos na leitura. */
@Service
public class CartService {

    private final CartRepository carts;
    private final ProductRepository products;

    public CartService(CartRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    public CartView view(UUID userId) {
        return toView(load(userId));
    }

    public CartView addItem(UUID userId, UUID productId, int quantity) {
        requireActiveProduct(productId);
        Cart cart = load(userId);
        cart.add(productId, quantity);
        carts.save(cart);
        return toView(cart);
    }

    public CartView setItemQuantity(UUID userId, UUID productId, int quantity) {
        if (quantity > 0) {
            requireActiveProduct(productId);
        }
        Cart cart = load(userId);
        cart.setQuantity(productId, quantity);
        persist(cart);
        return toView(cart);
    }

    public CartView removeItem(UUID userId, UUID productId) {
        Cart cart = load(userId);
        cart.remove(productId);
        persist(cart);
        return toView(cart);
    }

    public void clear(UUID userId) {
        carts.deleteByUserId(userId);
    }

    private Cart load(UUID userId) {
        return carts.findByUserId(userId).orElseGet(() -> Cart.empty(userId));
    }

    private void persist(Cart cart) {
        if (cart.isEmpty()) {
            carts.deleteByUserId(cart.userId());
        } else {
            carts.save(cart);
        }
    }

    private Product requireActiveProduct(UUID productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado"));
        if (!product.isActive()) {
            throw new ConflictException("Produto indisponível");
        }
        return product;
    }

    private CartView toView(Cart cart) {
        List<CartView.Line> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.items()) {
            Optional<Product> maybe = products.findById(item.productId());
            if (maybe.isEmpty() || !maybe.get().isActive()) {
                continue; // ignora itens cujo produto sumiu/ficou inativo
            }
            Product product = maybe.get();
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.quantity()));
            lines.add(new CartView.Line(product.getId(), product.getName(),
                    product.getPrice(), item.quantity(), lineTotal));
            total = total.add(lineTotal);
        }
        return new CartView(lines, total);
    }
}
