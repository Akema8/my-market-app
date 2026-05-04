package ru.yandex.practicum.mymarket.test.integration.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CheckoutService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.ProductService;

@WebFluxTest
@Import(SecurityConfig.class)
public abstract class BaseControllerTest {

    @Autowired
    protected WebTestClient webTestClient;

    @MockBean
    protected OrderService orderService;

    @MockBean
    protected CartService cartService;

    @MockBean
    protected CheckoutService checkoutService;

    @MockBean
    protected ProductService productService;
}
