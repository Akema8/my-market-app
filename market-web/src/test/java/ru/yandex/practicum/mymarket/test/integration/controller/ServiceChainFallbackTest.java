package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.controller.CartController;
import ru.yandex.practicum.mymarket.controller.CheckoutController;
import ru.yandex.practicum.mymarket.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CheckoutService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты реальной цепочки WebTestClient -> Controller -> Service -> PaymentClient.
 * Здесь CartService и CheckoutService настоящие, поэтому проверяется onErrorResume и fallback-поведение реактивных потоков.
 */
@WebFluxTest(controllers = {CartController.class, CheckoutController.class})
@Import({CartService.class, CheckoutService.class, ServiceChainFallbackTest.TestConfig.class, SecurityConfig.class})
class ServiceChainFallbackTest {

    @TestConfiguration
    static class TestConfig {
        @Bean("balanceCacheTtl")
        Duration balanceCacheTtl() {
            return Duration.ofSeconds(30);
        }
    }

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    ProductService productService;

    @MockBean
    PaymentClient paymentClient;

    @MockBean
    OrderService orderService;

    @MockBean
    @SuppressWarnings("unchecked")
    ReactiveRedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    private ReactiveValueOperations<String, Object> valueOps = mock(ReactiveValueOperations.class);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void getCartItems_RedisCastError_FallsBackToPaymentClient() {
        when(productService.getItemsInCart()).thenReturn(Flux.empty());
        when(valueOps.get("balance:1")).thenReturn(Mono.error(new ClassCastException("unexpected type in cache")));
        when(paymentClient.getBalance(1L)).thenReturn(Mono.just(new BalanceResponse(1L, 500L)));
        when(valueOps.set(anyString(), any(), any(Duration.class))).thenReturn(Mono.just(true));

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(paymentClient).getBalance(1L);
    }

    @Test
    void getCartItems_PaymentServiceDown_RendersPageWithZeroBalance() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 300L, 1);
        when(productService.getItemsInCart()).thenReturn(Flux.just(item));
        when(valueOps.get("balance:1")).thenReturn(Mono.empty());
        when(paymentClient.getBalance(1L)).thenReturn(Mono.empty());

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void checkout_CircuitBreakerFallback_RedirectsToCart() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 200L, 1);
        when(productService.getItemsInCart()).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(1L, 200L))
                .thenReturn(Mono.just(new PaymentResult(false, "Сервис платежей недоступен", null)));

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/cart/items.*");
    }
}
