package ru.yandex.practicum.mymarket.dto;

public record PaymentResult(
        Boolean success,
        String message,
        Long remainingBalance
) {
}
