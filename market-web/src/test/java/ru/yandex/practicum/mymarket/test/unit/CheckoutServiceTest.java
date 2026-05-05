package ru.yandex.practicum.mymarket.test.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.model.Cart;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CheckoutService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CART_ID = 1L;
    private static final String USERNAME = "user";

    @Mock private ProductService productService;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderService orderService;
    @Mock private CartService cartService;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        Cart mockCart = new Cart(USER_ID);
        mockCart.setId(CART_ID);
        when(cartService.findOrCreateCart(USERNAME)).thenReturn(Mono.just(mockCart));

        checkoutService = new CheckoutService(productService, paymentClient, orderService, cartService);
    }

    @Test
    void checkout_EmptyCart_ReturnsFailureImmediately() {
        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.errorMessage()).isEqualTo("Корзина пуста");
                })
                .verifyComplete();

        verify(paymentClient, never()).processPayment(any(), any());
        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    void checkout_PaymentDeclined_ReturnsFailureWithMessage() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 300L, 2);
        PaymentResult declined = new PaymentResult(false, "Недостаточно средств", null);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(USER_ID, 600L)).thenReturn(Mono.just(declined));

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.errorMessage()).isEqualTo("Недостаточно средств");
                })
                .verifyComplete();

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    void checkout_PaymentServiceDown_ReturnsFallbackFailure() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 200L, 1);
        PaymentResult unavailable = new PaymentResult(false, "Сервис платежей недоступен", null);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(USER_ID, 200L)).thenReturn(Mono.just(unavailable));

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.errorMessage()).contains("недоступен");
                })
                .verifyComplete();
    }

    @Test
    void checkout_Success_CreatesOrderAndEvictsBalanceCache() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 300L, 2);
        PaymentResult ok = new PaymentResult(true, "OK", 400L);

        when(productService.getItemsInCart(CART_ID)).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(USER_ID, 600L)).thenReturn(Mono.just(ok));
        when(orderService.createOrder(USER_ID, CART_ID)).thenReturn(Mono.just(42L));
        when(productService.clearCart(CART_ID)).thenReturn(Mono.empty());
        when(cartService.evictBalanceCache(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(checkoutService.checkout(USERNAME))
                .assertNext(r -> {
                    assertThat(r.success()).isTrue();
                    assertThat(r.orderId()).isEqualTo(42L);
                    assertThat(r.remainingBalance()).isEqualTo(400L);
                })
                .verifyComplete();

        verify(productService).clearCart(CART_ID);
        verify(cartService).evictBalanceCache(USER_ID);
    }
}
