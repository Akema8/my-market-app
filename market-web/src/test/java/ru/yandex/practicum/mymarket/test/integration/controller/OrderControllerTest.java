package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;

import java.util.List;

import static org.mockito.Mockito.when;

public class OrderControllerTest extends BaseControllerTest {

    @Test
    public void testGetOrders() {
        OrderDto order1 = new OrderDto(1L, List.of(), 20L);
        OrderDto order2 = new OrderDto(2L, List.of(), 10L);

        when(orderService.getAllOrders()).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetOrderPage() {
        OrderDto mockOrder = new OrderDto(10L, List.of(), 20L);
        when(orderService.getOrderById(10L)).thenReturn(Mono.just(mockOrder));

        webTestClient.get()
                .uri("/orders/10?newOrder=true")
                .exchange()
                .expectStatus().isOk();
    }
}