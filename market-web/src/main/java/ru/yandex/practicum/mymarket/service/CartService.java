package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.dto.CartSummary;

@Service
public class CartService {

    private final ProductService productService;
    private final PaymentClient paymentClient;

    public CartService(ProductService productService, PaymentClient paymentClient) {
        this.productService = productService;
        this.paymentClient = paymentClient;
    }

    public Mono<CartSummary> getCartSummary(Long userId) {
        return productService.getItemsInCart()
                .collectList()
                .flatMap(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();
                    return paymentClient.getBalance(userId)
                            .defaultIfEmpty(new BalanceResponse(userId, 0L))
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
}
