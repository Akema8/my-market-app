package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record CartSummary(
        List<ProductDto> items,
        long total,
        long balance,
        boolean hasSufficientFunds,
        boolean serviceAvailable
) {
}