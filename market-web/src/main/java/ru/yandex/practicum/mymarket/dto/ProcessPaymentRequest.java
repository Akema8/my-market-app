package ru.yandex.practicum.mymarket.dto;

public record ProcessPaymentRequest(
        Long userId,
        Long amount
) {
}
