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
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CheckoutService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CheckoutServiceTest {

    @Mock private ProductService productService;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderService orderService;
    @Mock private CartService cartService;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        checkoutService = new CheckoutService(productService, paymentClient, orderService, cartService);
    }

    @Test
    void checkout_EmptyCart_ReturnsFailureImmediately() {
        when(productService.getItemsInCart()).thenReturn(Flux.empty());

        StepVerifier.create(checkoutService.checkout(1L))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.errorMessage()).isEqualTo("Корзина пуста");
                })
                .verifyComplete();

        verify(paymentClient, never()).processPayment(any(), any());
        verify(orderService, never()).createOrder();
    }

    @Test
    void checkout_PaymentDeclined_ReturnsFailureWithMessage() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 300L, 2);
        PaymentResult declined = new PaymentResult(false, "Недостаточно средств", null);

        when(productService.getItemsInCart()).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(1L, 600L)).thenReturn(Mono.just(declined));

        StepVerifier.create(checkoutService.checkout(1L))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.errorMessage()).isEqualTo("Недостаточно средств");
                })
                .verifyComplete();

        verify(orderService, never()).createOrder();
    }

    @Test
    void checkout_PaymentServiceDown_ReturnsFallbackFailure() {
        ProductDto item = new ProductDto(1L, "Item", "", "", 200L, 1);
        PaymentResult unavailable = new PaymentResult(false, "Сервис платежей недоступен", null);

        when(productService.getItemsInCart()).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(1L, 200L)).thenReturn(Mono.just(unavailable));

        StepVerifier.create(checkoutService.checkout(1L))
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

        when(productService.getItemsInCart()).thenReturn(Flux.just(item));
        when(paymentClient.processPayment(1L, 600L)).thenReturn(Mono.just(ok));
        when(orderService.createOrder()).thenReturn(Mono.just(42L));
        when(productService.clearCart()).thenReturn(Mono.empty());
        when(cartService.evictBalanceCache(1L)).thenReturn(Mono.empty());

        StepVerifier.create(checkoutService.checkout(1L))
                .assertNext(r -> {
                    assertThat(r.success()).isTrue();
                    assertThat(r.orderId()).isEqualTo(42L);
                    assertThat(r.remainingBalance()).isEqualTo(400L);
                })
                .verifyComplete();

        verify(productService).clearCart();
        verify(cartService).evictBalanceCache(1L);
    }
}
