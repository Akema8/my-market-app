package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.dto.BalanceResponse;
import ru.yandex.practicum.payment.dto.PaymentResult;
import ru.yandex.practicum.payment.dto.ProcessPaymentRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {

    private PaymentService paymentService;

    @Mock
    private ReactiveRedisTemplate<String, Long> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, Long> valueOps;

    private static final Long INITIAL_BALANCE = 100000L;
    private static final Long USER_ID = 1L;
    private static final String BALANCE_KEY = "balance:user:" + USER_ID;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        paymentService = new PaymentService(redisTemplate, INITIAL_BALANCE);
    }

    @Test
    public void testGetBalance_ExistingUser() {
        Long existingBalance = 50000L;
        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.just(existingBalance));
        when(redisTemplate.hasKey(BALANCE_KEY)).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getUserId()).isEqualTo(USER_ID);
                    assertThat(response.getBalance()).isEqualTo(existingBalance);
                })
                .verifyComplete();

        verify(valueOps).get(BALANCE_KEY);
        verify(redisTemplate).hasKey(BALANCE_KEY);
        verify(valueOps, never()).set(anyString(), any());
    }

    @Test
    public void testGetBalance_NewUser_CreatesInitialBalance() {
        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.just(INITIAL_BALANCE));
        when(redisTemplate.hasKey(BALANCE_KEY)).thenReturn(Mono.just(false));
        when(valueOps.set(BALANCE_KEY, INITIAL_BALANCE)).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getUserId()).isEqualTo(USER_ID);
                    assertThat(response.getBalance()).isEqualTo(INITIAL_BALANCE);
                })
                .verifyComplete();

        verify(valueOps).get(BALANCE_KEY);
        verify(redisTemplate).hasKey(BALANCE_KEY);
        verify(valueOps).set(BALANCE_KEY, INITIAL_BALANCE);
    }

    @Test
    public void testGetBalance_EmptyRedis_UsesDefault() {
        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.empty());
        when(redisTemplate.hasKey(BALANCE_KEY)).thenReturn(Mono.just(false));
        when(valueOps.set(BALANCE_KEY, INITIAL_BALANCE)).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.getBalance(USER_ID))
                .assertNext(response -> {
                    assertThat(response.getUserId()).isEqualTo(USER_ID);
                    assertThat(response.getBalance()).isEqualTo(INITIAL_BALANCE);
                })
                .verifyComplete();
    }

    @Test
    public void testProcessPayment_Success() {
        Long currentBalance = 50000L;
        Long paymentAmount = 10000L;
        Long expectedNewBalance = 40000L;

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(USER_ID);
        request.setAmount(paymentAmount);

        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.just(currentBalance));
        when(valueOps.set(BALANCE_KEY, expectedNewBalance)).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.processPayment(request))
                .assertNext(result -> {
                    assertThat(result.getSuccess()).isTrue();
                    assertThat(result.getMessage()).isEqualTo("Успешная оплата!");
                    assertThat(result.getRemainingBalance()).isEqualTo(expectedNewBalance);
                })
                .verifyComplete();

        verify(valueOps).get(BALANCE_KEY);
        verify(valueOps).set(BALANCE_KEY, expectedNewBalance);
    }

    @Test
    public void testProcessPayment_InsufficientFunds() {
        Long currentBalance = 5000L;
        Long paymentAmount = 10000L;

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(USER_ID);
        request.setAmount(paymentAmount);

        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.just(currentBalance));

        StepVerifier.create(paymentService.processPayment(request))
                .assertNext(result -> {
                    assertThat(result.getSuccess()).isFalse();
                    assertThat(result.getMessage()).isEqualTo("Недостаточно средств :(");
                    assertThat(result.getRemainingBalance()).isNull();
                })
                .verifyComplete();

        verify(valueOps).get(BALANCE_KEY);
        verify(valueOps, never()).set(anyString(), anyLong());
    }

    @Test
    public void testProcessPayment_ExactAmount() {
        Long currentBalance = 10000L;
        Long paymentAmount = 10000L;
        Long expectedNewBalance = 0L;

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(USER_ID);
        request.setAmount(paymentAmount);

        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.just(currentBalance));
        when(valueOps.set(BALANCE_KEY, expectedNewBalance)).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.processPayment(request))
                .assertNext(result -> {
                    assertThat(result.getSuccess()).isTrue();
                    assertThat(result.getMessage()).isEqualTo("Успешная оплата!");
                    assertThat(result.getRemainingBalance()).isEqualTo(0L);
                })
                .verifyComplete();

        verify(valueOps).set(BALANCE_KEY, expectedNewBalance);
    }

    @Test
    public void testProcessPayment_NewUser_UsesInitialBalance() {
        Long paymentAmount = 10000L;
        Long expectedNewBalance = INITIAL_BALANCE - paymentAmount;

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(USER_ID);
        request.setAmount(paymentAmount);

        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.empty());
        when(valueOps.set(BALANCE_KEY, expectedNewBalance)).thenReturn(Mono.just(true));

        StepVerifier.create(paymentService.processPayment(request))
                .assertNext(result -> {
                    assertThat(result.getSuccess()).isTrue();
                    assertThat(result.getRemainingBalance()).isEqualTo(expectedNewBalance);
                })
                .verifyComplete();

        verify(valueOps).set(BALANCE_KEY, expectedNewBalance);
    }

    @Test
    public void testProcessPayment_LargeAmount() {
        Long currentBalance = 50000L;
        Long paymentAmount = 100000L; // Больше баланса

        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setUserId(USER_ID);
        request.setAmount(paymentAmount);

        when(valueOps.get(BALANCE_KEY)).thenReturn(Mono.just(currentBalance));

        StepVerifier.create(paymentService.processPayment(request))
                .assertNext(result -> {
                    assertThat(result.getSuccess()).isFalse();
                    assertThat(result.getMessage()).contains("Недостаточно средств");
                })
                .verifyComplete();
    }
}
