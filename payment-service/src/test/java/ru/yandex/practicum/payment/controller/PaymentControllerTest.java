package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.SecurityConfig;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentResult;
import ru.yandex.practicum.payment.dto.ProcessPaymentRequest;
import ru.yandex.practicum.payment.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
@Import(SecurityConfig.class)
public class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    @Test
    public void testGetBalance_Success() {
        Long userId = 1L;
        BalanceResponse response = new BalanceResponse();
        response.setUserId(userId);
        response.setBalance(50000L);

        when(paymentService.getBalance(userId)).thenReturn(Mono.just(response));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/balance")
                        .queryParam("userId", userId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.balance").isEqualTo(50000);
    }

    @Test
    public void testGetBalance_NewUser() {
        Long userId = 999L;
        BalanceResponse response = new BalanceResponse();
        response.setUserId(userId);
        response.setBalance(100000L);

        when(paymentService.getBalance(userId)).thenReturn(Mono.just(response));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .get()
                .uri("/balance?userId={userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(999)
                .jsonPath("$.balance").isEqualTo(100000);
    }

    @Test
    public void testProcessPayment_Success() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(1L);
        request.setAmount(10000L);

        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setMessage("Успешная оплата!");
        result.setRemainingBalance(40000L);

        when(paymentService.processPayment(any(ProcessPaymentRequest.class)))
                .thenReturn(Mono.just(result));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Успешная оплата!")
                .jsonPath("$.remainingBalance").isEqualTo(40000);
    }

    @Test
    public void testProcessPayment_InsufficientFunds() {
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(1L);
        request.setAmount(100000L);

        PaymentResult result = new PaymentResult();
        result.setSuccess(false);
        result.setMessage("Недостаточно средств :(");
        result.setRemainingBalance(null);

        when(paymentService.processPayment(any(ProcessPaymentRequest.class)))
                .thenReturn(Mono.just(result));

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Недостаточно средств :(");
    }

    @Test
    public void testProcessPayment_ValidationError_MissingUserId() {
        String requestBody = """
                {
                    "amount": 10000
                }
                """;

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testProcessPayment_ValidationError_MissingAmount() {
        String requestBody = """
                {
                    "userId": 1
                }
                """;

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testProcessPayment_ValidationError_NegativeAmount() {
        String requestBody = """
                {
                    "userId": 1,
                    "amount": -1000
                }
                """;

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testProcessPayment_ValidationError_ZeroAmount() {
        String requestBody = """
                {
                    "userId": 1,
                    "amount": 0
                }
                """;

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testGetBalance_MissingParameter() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .get()
                .uri("/balance")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testProcessPayment_InvalidJson() {
        String invalidJson = "{ invalid json }";

        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt())
                .post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void testGetBalance_Unauthorized() {
        webTestClient.get()
                .uri("/balance?userId=1")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
