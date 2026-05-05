package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.User;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class OrderControllerTest extends BaseControllerTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "user";

    @Test
    public void testGetOrders() {
        OrderDto order1 = new OrderDto(1L, List.of(), 20L);
        OrderDto order2 = new OrderDto(2L, List.of(), 10L);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(new User(USER_ID, USERNAME, "enc", true)));
        when(orderService.getAllOrders(USER_ID)).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetOrderPage() {
        OrderDto mockOrder = new OrderDto(10L, List.of(), 20L);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Mono.just(new User(USER_ID, USERNAME, "enc", true)));
        when(orderService.getOrderByIdForUser(10L, USER_ID)).thenReturn(Mono.just(mockOrder));

        webTestClient.get()
                .uri("/orders/10?newOrder=true")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getOrders_LooksUpUserByAuthenticatedUsernameBeforeLoadingOrders() {
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Mono.just(new User(USER_ID, USERNAME, "enc", true)));
        when(orderService.getAllOrders(USER_ID)).thenReturn(Flux.empty());

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk();

        verify(userRepository).findByUsername(USERNAME);
        verify(orderService).getAllOrders(USER_ID);
    }

    @Test
    @WithMockUser(username = "charlie")
    void getOrders_WithDifferentUser_LoadsOnlyThatUsersOrders() {
        Long charlieId = 77L;
        when(userRepository.findByUsername("charlie"))
                .thenReturn(Mono.just(new User(charlieId, "charlie", "enc", true)));
        when(orderService.getAllOrders(charlieId)).thenReturn(Flux.empty());

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk();

        verify(userRepository).findByUsername("charlie");
        verify(orderService).getAllOrders(charlieId);
        verify(orderService, never()).getAllOrders(USER_ID);
    }

    @Test
    void getOrderPage_LooksUpUserByAuthenticatedUsername() {
        OrderDto order = new OrderDto(5L, List.of(), 100L);
        when(userRepository.findByUsername(USERNAME))
                .thenReturn(Mono.just(new User(USER_ID, USERNAME, "enc", true)));
        when(orderService.getOrderByIdForUser(5L, USER_ID)).thenReturn(Mono.just(order));

        webTestClient.get().uri("/orders/5")
                .exchange()
                .expectStatus().isOk();

        verify(userRepository).findByUsername(USERNAME);
        verify(orderService).getOrderByIdForUser(5L, USER_ID);
    }
}
