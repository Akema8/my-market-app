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
import ru.yandex.practicum.mymarket.model.Cart;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;
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

    private static final Long USER_ID = 1L;
    private static final Long CART_ID = 1L;
    private static final String USERNAME = "user";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockBean private ProductService productService;
    @MockBean private PaymentClient paymentClient;
    @MockBean private UserRepository userRepository;
    @MockBean private CartRepository cartRepository;

    @Autowired private CartService cartService;
    @Autowired private ReactiveRedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.delete("balance:" + USER_ID).block();

        User mockUser = new User(USER_ID, USERNAME, "encoded", true);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(mockUser));

        Cart mockCart = new Cart(USER_ID);
        mockCart.setId(CART_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Mono.just(mockCart));
    }

    @Test
    void getCartSummary_BalanceCacheHit_DoesNotCallPaymentService() {
        BalanceResponse cached = new BalanceResponse(USER_ID, 750L);
        redisTemplate.opsForValue().set("balance:" + USER_ID, cached, Duration.ofSeconds(30)).block();

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(
                new ProductDto(1L, "Item", "", "", 100L, 1)));

        cartService.getCartSummary(USERNAME).block();

        verify(paymentClient, never()).getBalance(any());
    }

    @Test
    void getCartSummary_CacheMiss_FetchesAndStoresInRedis() {
        BalanceResponse balance = new BalanceResponse(USER_ID, 500L);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.empty());
        when(paymentClient.getBalance(USER_ID)).thenReturn(Mono.just(balance));

        cartService.getCartSummary(USERNAME).block();

        Object stored = redisTemplate.opsForValue().get("balance:" + USER_ID).block();
        assertThat(stored).isNotNull();
    }

    @Test
    void getCartSummary_ServiceDown_FallbackNotCached() {
        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.empty());
        when(paymentClient.getBalance(USER_ID)).thenReturn(Mono.empty());

        cartService.getCartSummary(USERNAME).block();

        Object stored = redisTemplate.opsForValue().get("balance:" + USER_ID).block();
        assertThat(stored).isNull();
    }

    @Test
    void evictBalanceCache_RemovesKeyFromRedis() {
        BalanceResponse balance = new BalanceResponse(USER_ID, 300L);
        redisTemplate.opsForValue().set("balance:" + USER_ID, balance, Duration.ofSeconds(30)).block();

        cartService.evictBalanceCache(USER_ID).block();

        Object stored = redisTemplate.opsForValue().get("balance:" + USER_ID).block();
        assertThat(stored).isNull();
    }

    @Test
    void checkout_AfterSuccessfulPayment_BalanceCacheEvicted() {
        redisTemplate.opsForValue().set("balance:" + USER_ID, new BalanceResponse(USER_ID, 1000L), Duration.ofSeconds(30)).block();

        cartService.evictBalanceCache(USER_ID).block();

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.empty());
        when(paymentClient.getBalance(USER_ID)).thenReturn(Mono.just(new BalanceResponse(USER_ID, 800L)));

        cartService.getCartSummary(USERNAME).block();

        verify(paymentClient).getBalance(USER_ID);
    }
}
