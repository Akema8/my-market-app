package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartSummary;
import ru.yandex.practicum.mymarket.dto.ProductDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class CartControllerTest extends BaseControllerTest {

    private static final String USERNAME = "user";

    @Test
    void getCartItems_ReturnsCartPage() {
        CartSummary summary = new CartSummary(List.of(), 0L, 1000L, true, true);
        when(cartService.getCartSummary(USERNAME)).thenReturn(Mono.just(summary));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCartItems_WithItems_ReturnsCartPage() {
        ProductDto item = new ProductDto(1L, "Book", "desc", "/img.jpg", 200L, 2);
        CartSummary summary = new CartSummary(List.of(item), 400L, 1000L, true, true);
        when(cartService.getCartSummary(USERNAME)).thenReturn(Mono.just(summary));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCartItems_ServiceUnavailable_StillReturnsCartPage() {
        CartSummary summary = new CartSummary(List.of(), 0L, 0L, false, false);
        when(cartService.getCartSummary(USERNAME)).thenReturn(Mono.just(summary));

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateCartItem_Increment_ReturnsCartPage() {
        ProductDto item = new ProductDto(1L, "Book", "desc", "/img.jpg", 200L, 3);
        CartSummary summary = new CartSummary(List.of(item), 600L, 1000L, true, true);
        when(cartService.updateItemAndGetSummary(eq(1L), eq("increment"), eq(USERNAME)))
                .thenReturn(Mono.just(summary));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "increment"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateCartItem_Decrement_ReturnsCartPage() {
        CartSummary summary = new CartSummary(List.of(), 0L, 1000L, true, true);
        when(cartService.updateItemAndGetSummary(eq(1L), eq("decrement"), eq(USERNAME)))
                .thenReturn(Mono.just(summary));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "decrement"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateCartItem_InsufficientFunds_StillReturnsCartPage() {
        ProductDto item = new ProductDto(1L, "Book", "desc", "/img.jpg", 200L, 10);
        CartSummary summary = new CartSummary(List.of(item), 2000L, 500L, false, true);
        when(cartService.updateItemAndGetSummary(any(), any(), eq(USERNAME)))
                .thenReturn(Mono.just(summary));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "increment"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCartItems_UsesAuthenticatedUsername() {
        CartSummary summary = new CartSummary(List.of(), 0L, 1000L, true, true);
        when(cartService.getCartSummary(USERNAME)).thenReturn(Mono.just(summary));

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCartSummary(USERNAME);
        verify(cartService, never()).getCartSummary("alice");
    }

    @Test
    @WithMockUser(username = "alice")
    void getCartItems_WithDifferentUser_UsesAlicesUsername() {
        CartSummary summary = new CartSummary(List.of(), 0L, 500L, true, true);
        when(cartService.getCartSummary("alice")).thenReturn(Mono.just(summary));

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();

        verify(cartService).getCartSummary("alice");
        verify(cartService, never()).getCartSummary(USERNAME);
    }

    @Test
    void updateCartItem_UsesAuthenticatedUsernameForCartLookup() {
        CartSummary summary = new CartSummary(List.of(), 0L, 1000L, true, true);
        when(cartService.updateItemAndGetSummary(eq(1L), eq("increment"), eq(USERNAME)))
                .thenReturn(Mono.just(summary));

        webTestClient.post()
                .uri("/cart/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "increment"))
                .exchange()
                .expectStatus().isOk();

        verify(cartService).updateItemAndGetSummary(1L, "increment", USERNAME);
    }
}
