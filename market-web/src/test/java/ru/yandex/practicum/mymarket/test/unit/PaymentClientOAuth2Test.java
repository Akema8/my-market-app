package ru.yandex.practicum.mymarket.test.unit;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.service.PaymentClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * market-web - payment-service
 * Проверяет, что WebClient добавляет Bearer-токен в заголовок Authorization на каждый вызов PaymentClient
 * ошибки получения токена или ответа 401
 */
class PaymentClientOAuth2Test {

    private static final ClientRegistration REGISTRATION = ClientRegistration
            .withRegistrationId("market-web-client")
            .clientId("test-client")
            .clientSecret("test-secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://keycloak/token")
            .build();

    private final AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();

    private CircuitBreakerRegistry cbRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        cbRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
    }


    private ReactiveOAuth2AuthorizedClientManager managerWithToken(String tokenValue) {
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, tokenValue,
                Instant.now(), Instant.now().plusSeconds(300));
        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(REGISTRATION, "system", token);
        return request -> Mono.just(client);
    }

    private PaymentClient clientReturning(ReactiveOAuth2AuthorizedClientManager manager,
                                          HttpStatus status,
                                          String responseBody) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("market-web-client");

        WebClient webClient = WebClient.builder()
                .baseUrl("http://payment-service")
                .filter(oauth2)
                .exchangeFunction(request -> {
                    capturedHeaders.set(request.headers());
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(responseBody)
                            .build());
                })
                .build();

        return new PaymentClient(webClient, cbRegistry, retryRegistry);
    }

    @Test
    void getBalance_SendsBearerTokenInAuthorizationHeader() {
        PaymentClient client = clientReturning(
                managerWithToken("test-access-token"),
                HttpStatus.OK,
                "{\"userId\":1,\"balance\":500}");

        StepVerifier.create(client.getBalance(1L))
                .assertNext(response -> assertThat(response.balance()).isEqualTo(500L))
                .verifyComplete();

        assertThat(capturedHeaders.get().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer test-access-token");
    }

    @Test
    void processPayment_SendsBearerTokenInAuthorizationHeader() {
        PaymentClient client = clientReturning(
                managerWithToken("payment-bearer-xyz"),
                HttpStatus.OK,
                "{\"success\":true,\"message\":\"OK\",\"orderId\":99}");

        StepVerifier.create(client.processPayment(1L, 300L))
                .assertNext(result -> assertThat(result.success()).isTrue())
                .verifyComplete();

        assertThat(capturedHeaders.get().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer payment-bearer-xyz");
    }

    @Test
    void getBalance_WhenTokenAcquisitionFails_ReturnsEmptyWithoutError() {
        ReactiveOAuth2AuthorizedClientManager failingManager =
                request -> Mono.error(new RuntimeException("Keycloak unreachable"));

        PaymentClient client = clientReturning(failingManager, HttpStatus.OK, "{}");

        StepVerifier.create(client.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void processPayment_WhenTokenAcquisitionFails_ReturnsFailureResult() {
        ReactiveOAuth2AuthorizedClientManager failingManager =
                request -> Mono.error(new RuntimeException("Keycloak unreachable"));

        PaymentClient client = clientReturning(failingManager, HttpStatus.OK, "{}");

        StepVerifier.create(client.processPayment(1L, 200L))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.message()).contains("недоступен");
                })
                .verifyComplete();
    }

    @Test
    void getBalance_WhenPaymentServiceReturns401_ReturnsEmptyWithoutError() {
        PaymentClient client = clientReturning(
                managerWithToken("valid-token"),
                HttpStatus.UNAUTHORIZED,
                "{}");

        StepVerifier.create(client.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void processPayment_WhenPaymentServiceReturns401_ReturnsFailureResult() {
        PaymentClient client = clientReturning(
                managerWithToken("valid-token"),
                HttpStatus.UNAUTHORIZED,
                "{}");

        StepVerifier.create(client.processPayment(1L, 200L))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.message()).contains("недоступен");
                })
                .verifyComplete();
    }
}
