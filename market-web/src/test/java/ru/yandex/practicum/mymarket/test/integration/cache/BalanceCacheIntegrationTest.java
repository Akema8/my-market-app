package ru.yandex.practicum.mymarket.test.integration.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.yandex.practicum.mymarket.config.RedisConfig;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = {RedisConfig.class, CartService.class}
)
@Testcontainers(disabledWithoutDocker = true)
class BalanceCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockBean
    private ProductService productService;

    @MockBean
    private PaymentClient paymentClient;

    @Autowired
    private CartService cartService;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void clearBalance() {
        redisTemplate.delete("balance:1").block();
    }

    @Test
    void getCartSummary_BalanceCacheHit_DoesNotCallPaymentService() {
        BalanceResponse cached = new BalanceResponse(1L, 750L);
        redisTemplate.opsForValue().set("balance:1", cached, Duration.ofSeconds(30)).block();

        when(productService.getItemsInCart()).thenReturn(Flux.just(
                new ProductDto(1L, "Item", "", "", 100L, 1)));

        cartService.getCartSummary(1L).block();

        verify(paymentClient, never()).getBalance(any());
    }

    @Test
    void getCartSummary_CacheMiss_FetchesAndStoresInRedis() {
        BalanceResponse balance = new BalanceResponse(1L, 500L);

        when(productService.getItemsInCart()).thenReturn(Flux.empty());
        when(paymentClient.getBalance(1L)).thenReturn(Mono.just(balance));

        cartService.getCartSummary(1L).block();

        Object stored = redisTemplate.opsForValue().get("balance:1").block();
        assertThat(stored).isNotNull();
    }

    @Test
    void getCartSummary_ServiceDown_FallbackNotCached() {
        when(productService.getItemsInCart()).thenReturn(Flux.empty());
        when(paymentClient.getBalance(1L)).thenReturn(Mono.empty());

        cartService.getCartSummary(1L).block();

        Object stored = redisTemplate.opsForValue().get("balance:1").block();
        assertThat(stored).isNull();
    }

    @Test
    void evictBalanceCache_RemovesKeyFromRedis() {
        BalanceResponse balance = new BalanceResponse(1L, 300L);
        redisTemplate.opsForValue().set("balance:1", balance, Duration.ofSeconds(30)).block();

        cartService.evictBalanceCache(1L).block();

        Object stored = redisTemplate.opsForValue().get("balance:1").block();
        assertThat(stored).isNull();
    }

    @Test
    void checkout_AfterSuccessfulPayment_BalanceCacheEvicted() {
        redisTemplate.opsForValue().set("balance:1", new BalanceResponse(1L, 1000L), Duration.ofSeconds(30)).block();

        cartService.evictBalanceCache(1L).block();

        when(productService.getItemsInCart()).thenReturn(Flux.empty());
        when(paymentClient.getBalance(1L)).thenReturn(Mono.just(new BalanceResponse(1L, 800L)));

        cartService.getCartSummary(1L).block();

        verify(paymentClient).getBalance(1L);
    }
}
