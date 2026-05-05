package ru.yandex.practicum.mymarket.test.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CartServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CART_ID = 1L;
    private static final String USERNAME = "user";

    @Mock private ProductService productService;
    @Mock private PaymentClient paymentClient;
    @Mock private ReactiveRedisTemplate<String, Object> redisTemplate;
    @Mock private ReactiveValueOperations<String, Object> valueOps;
    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        User mockUser = new User(USER_ID, USERNAME, "encoded", true);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(mockUser));

        Cart mockCart = new Cart(USER_ID);
        mockCart.setId(CART_ID);
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Mono.just(mockCart));

        cartService = new CartService(productService, paymentClient, redisTemplate, Duration.ofSeconds(30),
                userRepository, cartRepository);
    }

    @Test
    void getCartSummary_BalanceCacheHit_NoServiceCall() {
        BalanceResponse cached = new BalanceResponse(USER_ID, 1000L);
        ProductDto item = new ProductDto(1L, "Item", "", "", 200L, 1);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(item));
        when(valueOps.get("balance:" + USER_ID)).thenReturn(Mono.just(cached));

        StepVerifier.create(cartService.getCartSummary(USERNAME))
                .assertNext(s -> {
                    assertThat(s.balance()).isEqualTo(1000L);
                    assertThat(s.total()).isEqualTo(200L);
                    assertThat(s.hasSufficientFunds()).isTrue();
                })
                .verifyComplete();

        verify(paymentClient, never()).getBalance(any());
    }

    @Test
    void getCartSummary_CacheMiss_FetchesAndCachesBalance() {
        BalanceResponse balance = new BalanceResponse(USER_ID, 500L);
        ProductDto item = new ProductDto(1L, "Item", "", "", 600L, 1);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(item));
        when(valueOps.get("balance:" + USER_ID)).thenReturn(Mono.empty());
        when(paymentClient.getBalance(USER_ID)).thenReturn(Mono.just(balance));
        when(valueOps.set(anyString(), any(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(cartService.getCartSummary(USERNAME))
                .assertNext(s -> {
                    assertThat(s.balance()).isEqualTo(500L);
                    assertThat(s.hasSufficientFunds()).isFalse();
                })
                .verifyComplete();

        verify(paymentClient).getBalance(USER_ID);
        verify(valueOps).set(eq("balance:" + USER_ID), eq(balance), eq(Duration.ofSeconds(30)));
    }

    @Test
    void getCartSummary_ServiceUnavailable_UsesFallbackBalance() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 200L, 1);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(item));
        when(valueOps.get("balance:" + USER_ID)).thenReturn(Mono.empty());
        when(paymentClient.getBalance(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(cartService.getCartSummary(USERNAME))
                .assertNext(s -> {
                    assertThat(s.balance()).isEqualTo(0L);
                    assertThat(s.hasSufficientFunds()).isFalse();
                })
                .verifyComplete();

        verify(valueOps, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void getCartSummary_EmptyCart_SufficientFundsEvenWithZeroBalance() {
        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.empty());
        when(valueOps.get("balance:" + USER_ID)).thenReturn(Mono.just(new BalanceResponse(USER_ID, 0L)));

        StepVerifier.create(cartService.getCartSummary(USERNAME))
                .assertNext(s -> {
                    assertThat(s.items()).isEqualTo(List.of());
                    assertThat(s.total()).isEqualTo(0L);
                    assertThat(s.hasSufficientFunds()).isTrue();
                    assertThat(s.serviceAvailable()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void evictBalanceCache_DeletesKey() {
        when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));

        StepVerifier.create(cartService.evictBalanceCache(USER_ID))
                .verifyComplete();

        verify(redisTemplate).delete("balance:" + USER_ID);
    }
}
