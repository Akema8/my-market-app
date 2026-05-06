package ru.yandex.practicum.payment.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.config.SecurityConfig;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentResult;
import ru.yandex.practicum.payment.dto.ProcessPaymentRequest;
import ru.yandex.practicum.payment.service.PaymentService;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Проверяет JWT эндпоинтов payment-service
 */
@WebFluxTest(PaymentController.class)
@Import({SecurityConfig.class, PaymentAuthTest.TestJwtConfig.class})
class PaymentAuthTest {

    private static final String SECRET = "test-secret-key-for-jwt-testing-32bytes!!!!!!";
    private static final String CORRECT_AUDIENCE = "payment-service";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getBalance_WithoutJwt_Returns401() {
        webTestClient.get()
                .uri("/balance?userId=1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void processPayment_WithoutJwt_Returns401() {
        webTestClient.post()
                .uri("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId":1,"amount":1000}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_WithWrongAudience_Returns401() {
        webTestClient.get()
                .uri("/balance?userId=1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt("wrong-service"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void processPayment_WithWrongAudience_Returns401() {
        webTestClient.post()
                .uri("/payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt("wrong-service"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId":1,"amount":1000}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBalance_WithValidJwt_Returns200() {
        BalanceResponse response = new BalanceResponse();
        response.setUserId(1L);
        response.setBalance(50000L);
        when(paymentService.getBalance(anyLong())).thenReturn(Mono.just(response));

        webTestClient.get()
                .uri("/balance?userId=1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt(CORRECT_AUDIENCE))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.balance").isEqualTo(50000);
    }

    @Test
    void processPayment_WithValidJwt_Returns200() {
        PaymentResult result = new PaymentResult();
        result.setSuccess(true);
        result.setMessage("Успешная оплата!");
        result.setRemainingBalance(40000L);
        when(paymentService.processPayment(any(ProcessPaymentRequest.class)))
                .thenReturn(Mono.just(result));

        webTestClient.post()
                .uri("/payment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildJwt(CORRECT_AUDIENCE))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"userId":1,"amount":10000}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }

    private static String buildJwt(String audience) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
            MACSigner signer = new MACSigner(key);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("market-web-client")
                    .issuer("http://keycloak/realms/my-market")
                    .audience(audience)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                    .build();
            SignedJWT signed = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signed.sign(signer);
            return signed.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @TestConfiguration
    static class TestJwtConfig {

        private static final String SECRET = "test-secret-key-for-jwt-testing-32bytes!!!!!!";
        private static final String REQUIRED_AUDIENCE = "payment-service";

        @Bean
        @Primary
        ReactiveJwtDecoder testJwtDecoder() {
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
            NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                    .withSecretKey(key)
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();

            OAuth2TokenValidator<Jwt> audienceValidator = token -> {
                List<String> aud = token.getAudience();
                if (aud != null && aud.contains(REQUIRED_AUDIENCE)) {
                    return OAuth2TokenValidatorResult.success();
                }
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token",
                                "JWT audience must contain '" + REQUIRED_AUDIENCE + "'", null));
            };

            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefault(), audienceValidator));
            return decoder;
        }
    }
}
