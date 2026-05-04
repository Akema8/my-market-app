package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cache.CacheKeys;
import ru.yandex.practicum.mymarket.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.dto.CartSummary;

import java.time.Duration;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final ProductService productService;
    private final PaymentClient paymentClient;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final Duration balanceCacheTtl;

    public CartService(ProductService productService,
                       PaymentClient paymentClient,
                       ReactiveRedisTemplate<String, Object> redisTemplate,
                       @Qualifier("balanceCacheTtl") Duration balanceCacheTtl) {
        this.productService = productService;
        this.paymentClient = paymentClient;
        this.redisTemplate = redisTemplate;
        this.balanceCacheTtl = balanceCacheTtl;
    }

    public Mono<CartSummary> getCartSummary(Long userId) {
        return productService.getItemsInCart()
                .collectList()
                .flatMap(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();
                    return getBalanceCached(userId)
                            .map(balance -> new CartSummary(
                                    cartItems,
                                    total,
                                    balance.balance(),
                                    balance.balance() >= total,
                                    balance.balance() > 0 || total == 0
                            ));
                });
    }

    public Mono<CartSummary> updateItemAndGetSummary(Long productId, String action, Long userId) {
        return productService.changeItemQuantity(productId, action)
                .then(getCartSummary(userId));
    }

    public Mono<Void> evictBalanceCache(Long userId) {
        log.info("Evicting balance cache for user {}", userId);
        return redisTemplate.delete(CacheKeys.balance(userId)).then();
    }

    private Mono<BalanceResponse> getBalanceCached(Long userId) {
        String key = CacheKeys.balance(userId);
        return redisTemplate.opsForValue()
                .get(key)
                .cast(BalanceResponse.class)
                .doOnNext(b -> log.info("Balance cache hit for user {}: {}", userId, b.balance()))
                .onErrorResume(e -> {
                    log.warn("Balance cache read failed for user {}, fetching from service", userId);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() ->
                        paymentClient.getBalance(userId)
                                .flatMap(balance -> redisTemplate.opsForValue()
                                        .set(key, balance, balanceCacheTtl)
                                        .thenReturn(balance)
                                        .onErrorReturn(balance))
                                .doOnNext(b -> log.info("Balance fetched and cached for user {}: {}", userId, b.balance()))
                                .switchIfEmpty(Mono.just(new BalanceResponse(userId, 0L)))
                ));
    }
}
