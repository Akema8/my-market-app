package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.model.Cart;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CheckoutService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.ProductService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest
@Import(SecurityConfig.class)
@WithMockUser
public abstract class BaseControllerTest {

    @Autowired
    protected WebTestClient webTestClient;

    @MockBean
    protected ReactiveUserDetailsService userDetailsService;

    @MockBean
    protected UserRepository userRepository;

    @MockBean
    protected OrderService orderService;

    @MockBean
    protected CartService cartService;

    @MockBean
    protected CheckoutService checkoutService;

    @MockBean
    protected ProductService productService;

    @BeforeEach
    void setUpCartMock() {
        webTestClient = webTestClient.mutateWith(SecurityMockServerConfigurers.csrf());
        Cart mockCart = new Cart(1L);
        mockCart.setId(1L);
        when(cartService.findOrCreateCart(anyString())).thenReturn(Mono.just(mockCart));
    }
}
