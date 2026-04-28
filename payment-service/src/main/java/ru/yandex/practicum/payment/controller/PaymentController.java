package ru.yandex.practicum.payment.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.ProcessPaymentRequest;
import ru.yandex.practicum.payment.dto.PaymentResult;
import ru.yandex.practicum.payment.service.PaymentService;

@RestController
@RequestMapping("/")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/balance")
    public Mono<BalanceResponse> getBalance(@RequestParam Long userId) {
        log.info("GET /balance - userId={}", userId);
        return paymentService.getBalance(userId);
    }

    @PostMapping("/payment")
    @ResponseStatus(HttpStatus.OK)
    public Mono<PaymentResult> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        log.info("POST /payment - userId={}, amount={}", request.getUserId(), request.getAmount());
        return paymentService.processPayment(request);
    }
}
