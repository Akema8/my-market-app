package ru.yandex.practicum.mymarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

@Controller
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private static final Long USER_ID = 1L;

    private final OrderService orderService;
    private final PaymentClient paymentClient;
    private final ProductService productService;

    public CheckoutController(OrderService orderService, PaymentClient paymentClient, ProductService productService) {
        this.orderService = orderService;
        this.paymentClient = paymentClient;
        this.productService = productService;
    }

    @PostMapping("/buy")
    public Mono<String> checkout(WebSession session) {
        return productService.getItemsInCart()
                .collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        session.getAttributes().put("error", "Корзина пуста");
                        return Mono.just("redirect:/cart/items");
                    }

                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();

                    log.info("Processing checkout for user {}, total amount: {}", USER_ID, total);

                    return paymentClient.processPayment(USER_ID, total)
                            .flatMap(paymentResult -> {
                                if (!paymentResult.success()) {
                                    log.warn("Payment failed: {}", paymentResult.message());
                                    session.getAttributes().put("error", paymentResult.message());
                                    return Mono.just("redirect:/cart/items");
                                }

                                log.info("Payment successful, creating order");
                                return orderService.createOrder()
                                        .flatMap(orderId -> {
                                            log.info("Order created: {}, clearing cart", orderId);
                                            return productService.clearCart()
                                                    .thenReturn(orderId);
                                        })
                                        .map(orderId -> {
                                            session.getAttributes().put("success",
                                                    "Заказ успешно оформлен! Остаток на балансе: " + paymentResult.remainingBalance() + " руб.");
                                            return "redirect:/orders/" + orderId + "?newOrder=true";
                                        });
                            });
                });
    }
}
