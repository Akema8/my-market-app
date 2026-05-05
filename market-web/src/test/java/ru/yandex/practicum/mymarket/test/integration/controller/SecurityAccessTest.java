package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.dto.CartSummary;
import ru.yandex.practicum.mymarket.model.Cart;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.CheckoutService;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Павила доступа Spring Security:
 * анонимные пользователи видят публичные страницы и перенаправляются с защищённых;
 * аутентифицированные пользователи получают доступ ко всем страницам;
 * форм-логин (POST /login) корректно аутентифицирует и отклоняет;
 * выход (POST /logout) инвалидирует сессию.
 */
@WebFluxTest
@Import(SecurityConfig.class)
class SecurityAccessTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveUserDetailsService userDetailsService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private CartService cartService;
    @MockBean
    private ProductService productService;
    @MockBean
    private OrderService orderService;
    @MockBean
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        Cart mockCart = new Cart(1L);
        mockCart.setId(1L);
        when(cartService.findOrCreateCart(anyString())).thenReturn(Mono.just(mockCart));
        when(productService.findItems(any(), anyString(), anyInt(), anyInt(), any()))
                .thenReturn(Mono.just(new PageImpl<>(List.of())));
        when(cartService.getCartSummary(anyString()))
                .thenReturn(Mono.just(new CartSummary(List.of(), 0L, 1000L, true, true)));
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Mono.just(new User(1L, "user", "enc", true)));
        when(orderService.getAllOrders(anyLong())).thenReturn(Flux.empty());
    }

    @Test
    @WithAnonymousUser
    void anonymous_GetItems_IsAllowed() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithAnonymousUser
    void anonymous_GetLogin_IsAllowed() {
        webTestClient.get().uri("/login")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithAnonymousUser
    void anonymous_GetRegister_IsAllowed() {
        webTestClient.get().uri("/register")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithAnonymousUser
    void anonymous_GetCart_RedirectsToLogin() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc -> assertThat(loc).contains("/login"));
    }

    @Test
    @WithAnonymousUser
    void anonymous_GetOrders_RedirectsToLogin() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc -> assertThat(loc).contains("/login"));
    }

    @Test
    @WithAnonymousUser
    void anonymous_PostBuy_WithCsrf_RedirectsToLogin() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc -> assertThat(loc).contains("/login"));
    }

    @Test
    @WithMockUser
    void authenticated_GetCart_IsAllowed() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser
    void authenticated_GetOrders_IsAllowed() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithAnonymousUser
    void formLogin_WithCorrectCredentials_RedirectsToItems() {
        String rawPassword = "secret123";
        String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("buyer")
                .password(encodedPassword)
                .roles("USER")
                .build();
        when(userDetailsService.findByUsername("buyer")).thenReturn(Mono.just(user));

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "buyer").with("password", rawPassword))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc -> assertThat(loc).contains("/items"));
    }

    @Test
    @WithAnonymousUser
    void formLogin_WithWrongPassword_RedirectsToLoginWithErrorParam() {
        String encodedPassword = new BCryptPasswordEncoder().encode("correct-password");
        UserDetails user = org.springframework.security.core.userdetails.User
                .withUsername("buyer")
                .password(encodedPassword)
                .roles("USER")
                .build();
        when(userDetailsService.findByUsername("buyer")).thenReturn(Mono.just(user));

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "buyer").with("password", "wrong-password"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc ->
                        assertThat(loc).contains("/login").contains("error"));
    }

    @Test
    @WithAnonymousUser
    void formLogin_WithUnknownUser_RedirectsToLoginWithErrorParam() {
        when(userDetailsService.findByUsername("ghost"))
                .thenReturn(Mono.error(
                        new org.springframework.security.core.userdetails.UsernameNotFoundException("ghost")));

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "ghost").with("password", "any"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc ->
                        assertThat(loc).contains("/login").contains("error"));
    }

    @Test
    @WithAnonymousUser
    void formLogin_WithoutCsrfToken_Returns403() {
        webTestClient.post().uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "buyer").with("password", "secret"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser
    void logout_RedirectsToLoginWithLogoutParam() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/logout")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc ->
                        assertThat(loc).contains("/login").contains("logout"));
    }
}
