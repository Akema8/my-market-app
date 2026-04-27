package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.controller.OrderController;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private OrderService orderService;

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