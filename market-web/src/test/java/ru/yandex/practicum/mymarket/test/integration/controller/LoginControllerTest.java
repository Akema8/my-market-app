package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.config.SecurityConfig;
import ru.yandex.practicum.mymarket.controller.LoginController;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(controllers = LoginController.class)
@Import(SecurityConfig.class)
class LoginControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveUserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void loginPage_WithNoParams_ShowsCleanForm() {
        webTestClient.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> {
                    assertThat(html).contains("Войти");
                    assertThat(html).doesNotContain("Неверный логин или пароль");
                    assertThat(html).doesNotContain("Вы вышли из системы");
                    assertThat(html).doesNotContain("Регистрация прошла успешно");
                });
    }

    @Test
    void loginPage_WithErrorParam_ShowsOnlyErrorAlert() {
        webTestClient.get().uri("/login?error")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> {
                    assertThat(html).contains("Неверный логин или пароль");
                    assertThat(html).doesNotContain("Вы вышли из системы");
                    assertThat(html).doesNotContain("Регистрация прошла успешно");
                });
    }

    @Test
    void loginPage_WithLogoutParam_ShowsOnlyLogoutAlert() {
        webTestClient.get().uri("/login?logout")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> {
                    assertThat(html).contains("Вы вышли из системы");
                    assertThat(html).doesNotContain("Неверный логин или пароль");
                    assertThat(html).doesNotContain("Регистрация прошла успешно");
                });
    }

    @Test
    void loginPage_WithRegisteredParam_ShowsOnlyRegisteredAlert() {
        webTestClient.get().uri("/login?registered")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(html -> {
                    assertThat(html).contains("Регистрация прошла успешно");
                    assertThat(html).doesNotContain("Неверный логин или пароль");
                    assertThat(html).doesNotContain("Вы вышли из системы");
                });
    }
}
