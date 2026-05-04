package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CheckoutResult;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final ProductService productService;
    private final PaymentClient paymentClient;
    private final OrderService orderService;
    private final CartService cartService;

    public CheckoutService(ProductService productService,
                           PaymentClient paymentClient,
                           OrderService orderService,
                           CartService cartService) {
        this.productService = productService;
        this.paymentClient = paymentClient;
        this.orderService = orderService;
        this.cartService = cartService;
    }

    public Mono<CheckoutResult> checkout(Long userId) {
        return productService.getItemsInCart()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.just(CheckoutResult.failure("Корзина пуста"));
                    }

                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();

                    log.info("Processing checkout for user {}, total amount: {}", userId, total);

                    return paymentClient.processPayment(userId, total)
                            .flatMap(paymentResult -> {
                                if (!paymentResult.success()) {
                                    log.warn("Payment failed: {}", paymentResult.message());
                                    return Mono.just(CheckoutResult.failure(paymentResult.message()));
                                }

                                log.info("Payment successful, creating order");
                                return orderService.createOrder()
                                        .flatMap(orderId -> {
                                            log.info("Order created: {}, clearing cart and balance cache", orderId);
                                            return productService.clearCart()
                                                    .then(cartService.evictBalanceCache(userId))
                                                    .thenReturn(orderId);
                                        })
                                        .map(orderId -> {
                                            log.info("Checkout complete, order: {}", orderId);
                                            return CheckoutResult.success(orderId, paymentResult.remainingBalance());
                                        });
                            });
                });
    }
}
