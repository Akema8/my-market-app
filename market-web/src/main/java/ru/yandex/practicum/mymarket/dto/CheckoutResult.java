package ru.yandex.practicum.mymarket.dto;

public record CheckoutResult(
        boolean success,
        Long orderId,
        Long remainingBalance,
        String errorMessage
) {
    public static CheckoutResult success(Long orderId, Long remainingBalance) {
        return new CheckoutResult(true, orderId, remainingBalance, null);
    }

    public static CheckoutResult failure(String errorMessage) {
        return new CheckoutResult(false, null, null, errorMessage);
    }
}