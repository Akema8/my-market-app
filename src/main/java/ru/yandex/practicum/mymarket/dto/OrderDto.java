package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public record OrderDto(Long id, List<OrderItemDto> items, Long totalSum) {}