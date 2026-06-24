package dev.adastratech.mercuryshop.cart.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adastratech.mercuryshop.cart.domain.Cart;
import dev.adastratech.mercuryshop.cart.domain.CartItem;
import dev.adastratech.mercuryshop.cart.domain.CartRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Carrinho persistido no Redis como JSON, com TTL (estilo sessão). */
@Component
class RedisCartRepository implements CartRepository {

    private static final String PREFIX = "cart:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    RedisCartRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Cart> findByUserId(UUID userId) {
        String json = redis.opsForValue().get(PREFIX + userId);
        if (json == null) {
            return Optional.empty();
        }
        CartData data = read(json);
        Map<UUID, Integer> items = new LinkedHashMap<>();
        data.items().forEach(item -> items.put(item.productId(), item.quantity()));
        return Optional.of(Cart.of(userId, items));
    }

    @Override
    public void save(Cart cart) {
        List<CartData.Item> items = cart.items().stream()
                .map(i -> new CartData.Item(i.productId(), i.quantity()))
                .toList();
        redis.opsForValue().set(PREFIX + cart.userId(), write(new CartData(items)), TTL);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        redis.delete(PREFIX + userId);
    }

    private String write(CartData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar o carrinho", e);
        }
    }

    private CartData read(String json) {
        try {
            return objectMapper.readValue(json, CartData.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar o carrinho", e);
        }
    }

    /** Representação de persistência (independente do modelo de domínio). */
    private record CartData(List<Item> items) {
        private record Item(UUID productId, int quantity) {
        }
    }
}
