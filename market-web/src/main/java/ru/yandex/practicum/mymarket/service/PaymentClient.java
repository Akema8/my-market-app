package ru.yandex.practicum.mymarket.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.BalanceResponse;
import ru.yandex.practicum.mymarket.dto.PaymentResult;
import ru.yandex.practicum.mymarket.dto.ProcessPaymentRequest;

import java.time.Duration;

@Service
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient paymentWebClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry readRetry;

    public PaymentClient(WebClient paymentWebClient,
                         CircuitBreakerRegistry circuitBreakerRegistry,
                         RetryRegistry retryRegistry) {
        this.paymentWebClient = paymentWebClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentService");
        this.readRetry = retryRegistry.retry("paymentServiceRead");
    }

    public Mono<BalanceResponse> getBalance(Long userId) {
        return paymentWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToMono(BalanceResponse.class)
                .timeout(TIMEOUT)
                .doOnNext(response -> log.info("Balance for user {} = {}", userId, response.balance()))
                // Retry on transient failures (exponential backoff), then CB sees the final outcome
                .transformDeferred(RetryOperator.of(readRetry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(CallNotPermittedException.class, ex -> {
                    log.warn("Payment service circuit open, skipping balance check");
                    return Mono.empty();
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Payment service HTTP error getting balance: {}", ex.getStatusCode());
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Payment service unavailable: {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    public Mono<PaymentResult> processPayment(Long userId, Long amount) {
        ProcessPaymentRequest request = new ProcessPaymentRequest(userId, amount);
        return paymentWebClient.post()
                .uri("/payment")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResult.class)
                .timeout(TIMEOUT)
                .doOnNext(response -> log.info("Payment result for user {}: success={}", userId, response.success()))
                // No retry — processPayment is not idempotent (risk of double charge)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(CallNotPermittedException.class, ex -> {
                    log.warn("Payment service circuit open, payment not attempted");
                    return Mono.just(new PaymentResult(false, "Сервис платежей временно недоступен", null));
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Payment service HTTP error processing payment: {}", ex.getStatusCode());
                    return Mono.just(new PaymentResult(false, "Сервис платежей недоступен", null));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Payment service unavailable: {}", ex.getMessage());
                    return Mono.just(new PaymentResult(false, "Сервис платежей недоступен", null));
                });
    }
}
