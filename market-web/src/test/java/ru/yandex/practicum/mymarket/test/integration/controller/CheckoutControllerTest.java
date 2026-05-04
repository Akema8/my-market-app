package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CheckoutResult;

import static org.mockito.Mockito.when;

public class CheckoutControllerTest extends BaseControllerTest {

    @Test
    void checkout_Success_RedirectsToNewOrder() {
        when(checkoutService.checkout(1L))
                .thenReturn(Mono.just(CheckoutResult.success(42L, 800L)));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/orders/42.*");
    }

    @Test
    void checkout_EmptyCart_RedirectsToCart() {
        when(checkoutService.checkout(1L))
                .thenReturn(Mono.just(CheckoutResult.failure("Корзина пуста")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/cart/items.*");
    }

    @Test
    void checkout_PaymentDeclined_RedirectsToCart() {
        when(checkoutService.checkout(1L))
                .thenReturn(Mono.just(CheckoutResult.failure("Недостаточно средств")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/cart/items.*");
    }

    @Test
    void checkout_ServiceUnavailable_RedirectsToCart() {
        when(checkoutService.checkout(1L))
                .thenReturn(Mono.just(CheckoutResult.failure("Сервис платежей недоступен")));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/cart/items.*");
    }
}
