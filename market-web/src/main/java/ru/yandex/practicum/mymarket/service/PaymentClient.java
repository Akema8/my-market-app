package ru.yandex.practicum.mymarket.service;

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

    public PaymentClient(WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
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
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Payment service error while getting balance: {}", ex.getMessage());
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
                .doOnNext(response -> log.info("Payment result for user {}: success={}, message={}", userId, response.success(), response.message()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Payment service error while processing payment: {}", ex.getMessage());
                    return Mono.just(new PaymentResult(false, "Сервис платежей недоступен", null));
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Payment service unavailable: {}", ex.getMessage());
                    return Mono.just(new PaymentResult(false, "Сервис платежей недоступен", null));
                });
    }
}
