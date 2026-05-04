package ru.yandex.practicum.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.ProcessPaymentRequest;
import ru.yandex.practicum.payment.dto.PaymentResult;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String BALANCE_KEY_PREFIX = "balance:user:";

    private final ReactiveRedisTemplate<String, Long> redisTemplate;
    private final Long initialBalance;

    public PaymentService(ReactiveRedisTemplate<String, Long> redisTemplate, Long initialBalance) {
        this.redisTemplate = redisTemplate;
        this.initialBalance = initialBalance;
    }

    public Mono<BalanceResponse> getBalance(Long userId) {
        String key = BALANCE_KEY_PREFIX + userId;

        return redisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty(initialBalance)
                .flatMap(balance -> {
                    return redisTemplate.hasKey(key)
                            .flatMap(exists -> {
                                if (Boolean.FALSE.equals(exists)) {
                                    log.info("Creating initial balance {} for user {}", initialBalance, userId);
                                    return redisTemplate.opsForValue()
                                            .set(key, initialBalance)
                                            .thenReturn(initialBalance);
                                }
                                return Mono.just(balance);
                            });
                })
                .map(balance -> {
                    BalanceResponse response = new BalanceResponse();
                    response.setUserId(userId);
                    response.setBalance(balance);
                    return response;
                })
                .doOnNext(dto -> log.info("Balance for user {}: {}", userId, dto.getBalance()));
    }

    public Mono<PaymentResult> processPayment(ProcessPaymentRequest request) {
        String key = BALANCE_KEY_PREFIX + request.getUserId();

        log.info("Processing payment: userId={}, amount={}", request.getUserId(), request.getAmount());

        return redisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty(initialBalance)
                .flatMap(currentBalance -> {
                    long newBalance = currentBalance - request.getAmount();

                    if (newBalance < 0) {
                        log.warn("Insufficient funds for user {}: balance={}, amount={}",
                                request.getUserId(), currentBalance, request.getAmount());
                        return Mono.just(createPaymentResult(false, "Недостаточно средств :(", null));
                    }

                    return redisTemplate.opsForValue()
                            .set(key, newBalance)
                            .thenReturn(createPaymentResult(true, "Успешная оплата!", newBalance))
                            .doOnNext(response -> log.info("Payment successful for user {}: new balance={}",
                                    request.getUserId(), newBalance));
                });
    }

    private PaymentResult createPaymentResult(boolean success, String message, Long remainingBalance) {
        PaymentResult result = new PaymentResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setRemainingBalance(remainingBalance);
        return result;
    }
}
