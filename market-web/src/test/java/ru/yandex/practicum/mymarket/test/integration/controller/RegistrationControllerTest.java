package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.controller.RegistrationController;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Проверяет регистрацию пользователей
 */
@WebFluxTest(controllers = RegistrationController.class)
@Import(SecurityConfig.class)
class RegistrationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveUserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getRegisterPage_IsPublicAndReturns200() {
        webTestClient.get().uri("/register")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void register_NewUser_RedirectsToLoginWithRegisteredParam() {
        when(userRepository.findByUsername("alice")).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(Mono.just(new User(1L, "alice", "encoded", true)));

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "alice").with("password", "pass1234"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc ->
                        assertThat(loc).contains("/login").contains("registered"));
    }

    @Test
    void register_ExistingUser_Returns200WithErrorMessage() {
        when(userRepository.findByUsername("buyer"))
                .thenReturn(Mono.just(new User(1L, "buyer", "enc", true)));

        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "buyer").with("password", "pass1234"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html ->
                        assertThat(html).contains("уже существует"));
    }

    @Test
    void register_WithBlankUsername_Returns200WithValidationError() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "").with("password", "pass1234"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html ->
                        assertThat(html).contains("не могут быть пустыми"));
    }

    @Test
    void register_WithBlankPassword_Returns200WithValidationError() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.csrf())
                .post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "alice").with("password", ""))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html ->
                        assertThat(html).contains("не могут быть пустыми"));
    }

    @Test
    void register_WithoutCsrfToken_Returns403() {
        webTestClient.post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "alice").with("password", "pass1234"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
