package ru.yandex.practicum.mymarket.test.unit;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.service.PaymentClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentClientTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .ignoreExceptions(WebClientResponseException.class)
                .build());
    }

    private PaymentClient clientWith(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new PaymentClient(webClient, circuitBreakerRegistry, retryRegistry);
    }

    @Test
    void getBalance_Success_ReturnsBalance() {
        PaymentClient client = clientWith(req -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("""
                                {"userId":1,"balance":1000}""")
                        .build()
        ));

        StepVerifier.create(client.getBalance(1L))
                .assertNext(b -> assertThat(b.balance()).isEqualTo(1000L))
                .verifyComplete();
    }

    @Test
    void getBalance_NetworkError_ReturnsEmpty() {
        PaymentClient client = clientWith(req -> Mono.error(new IOException("Connection refused")));

        StepVerifier.create(client.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void getBalance_Timeout_ReturnsEmpty() {
        PaymentClient client = clientWith(req -> Mono.error(new TimeoutException("Timed out")));

        StepVerifier.create(client.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void getBalance_Http500_ReturnsEmpty() {
        PaymentClient client = clientWith(req -> Mono.just(
                ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()
        ));

        StepVerifier.create(client.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void getBalance_CircuitOpen_FastFailsWithEmpty() {
        circuitBreakerRegistry.circuitBreaker("paymentService").transitionToOpenState();

        PaymentClient client = clientWith(req -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("""
                                {"userId":1,"balance":1000}""")
                        .build()
        ));

        StepVerifier.create(client.getBalance(1L))
                .verifyComplete();
    }

    @Test
    void processPayment_Success_ReturnsResult() {
        PaymentClient client = clientWith(req -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("""
                                {"success":true,"message":"OK","remainingBalance":800}""")
                        .build()
        ));

        StepVerifier.create(client.processPayment(1L, 200L))
                .assertNext(r -> {
                    assertThat(r.success()).isTrue();
                    assertThat(r.remainingBalance()).isEqualTo(800L);
                })
                .verifyComplete();
    }

    @Test
    void processPayment_NetworkError_ReturnsFallback() {
        PaymentClient client = clientWith(req -> Mono.error(new IOException("Connection refused")));

        StepVerifier.create(client.processPayment(1L, 200L))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.message()).contains("недоступен");
                })
                .verifyComplete();
    }

    @Test
    void processPayment_Http500_ReturnsFallback() {
        PaymentClient client = clientWith(req -> Mono.just(
                ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()
        ));

        StepVerifier.create(client.processPayment(1L, 200L))
                .assertNext(r -> assertThat(r.success()).isFalse())
                .verifyComplete();
    }

    @Test
    void processPayment_CircuitOpen_ReturnsFallbackImmediately() {
        circuitBreakerRegistry.circuitBreaker("paymentService").transitionToOpenState();

        PaymentClient client = clientWith(req -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("""
                                {"success":true,"message":"OK","remainingBalance":800}""")
                        .build()
        ));

        StepVerifier.create(client.processPayment(1L, 200L))
                .assertNext(r -> {
                    assertThat(r.success()).isFalse();
                    assertThat(r.message()).contains("недоступен");
                })
                .verifyComplete();
    }
}
