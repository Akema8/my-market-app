package ru.yandex.practicum.mymarket.test.integration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

@WebFluxTest
public abstract class BaseControllerTest {

    @Autowired
    protected WebTestClient webTestClient;

    @MockBean
    protected OrderService orderService;

    @MockBean
    protected PaymentClient paymentClient;

    @MockBean
    protected ProductService productService;
}
