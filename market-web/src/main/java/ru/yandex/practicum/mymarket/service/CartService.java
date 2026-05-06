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
import ru.yandex.practicum.mymarket.model.Cart;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import java.time.Duration;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final ProductService productService;
    private final PaymentClient paymentClient;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final Duration balanceCacheTtl;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

    public CartService(ProductService productService,
                       PaymentClient paymentClient,
                       ReactiveRedisTemplate<String, Object> redisTemplate,
                       @Qualifier("balanceCacheTtl") Duration balanceCacheTtl,
                       UserRepository userRepository,
                       CartRepository cartRepository) {
        this.productService = productService;
        this.paymentClient = paymentClient;
        this.redisTemplate = redisTemplate;
        this.balanceCacheTtl = balanceCacheTtl;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
    }

    public Mono<Cart> findOrCreateCart(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> cartRepository.findByUserId(user.getId())
                        .switchIfEmpty(Mono.defer(() ->
                                cartRepository.save(new Cart(user.getId()))
                                        .doOnNext(c -> log.info("Created cart {} for user {}", c.getId(), user.getId()))
                        ))
                );
    }

    public Mono<CartSummary> getCartSummary(String username) {
        return findOrCreateCart(username)
                .flatMap(cart ->
                        productService.getItemsInCart(cart.getId())
                                .collectList()
                                .flatMap(cartItems -> {
                                    long total = cartItems.stream()
                                            .mapToLong(item -> item.price() * item.count())
                                            .sum();
                                    return getBalanceCached(cart.getUserId())
                                            .map(balance -> new CartSummary(
                                                    cartItems,
                                                    total,
                                                    balance.balance(),
                                                    balance.balance() >= total,
                                                    balance.balance() > 0 || total == 0
                                            ));
                                })
                );
    }

    public Mono<CartSummary> updateItemAndGetSummary(Long productId, String action, String username) {
        return findOrCreateCart(username)
                .flatMap(cart ->
                        productService.changeItemQuantity(productId, action, cart.getId())
                                .then(getCartSummaryByCart(cart))
                );
    }

    public Mono<Void> evictBalanceCache(Long userId) {
        log.info("Evicting balance cache for user {}", userId);
        return redisTemplate.delete(CacheKeys.balance(userId)).then();
    }

    private Mono<CartSummary> getCartSummaryByCart(Cart cart) {
        return productService.getItemsInCart(cart.getId())
                .collectList()
                .flatMap(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();
                    return getBalanceCached(cart.getUserId())
                            .map(balance -> new CartSummary(
                                    cartItems,
                                    total,
                                    balance.balance(),
                                    balance.balance() >= total,
                                    balance.balance() > 0 || total == 0
                            ));
                });
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
