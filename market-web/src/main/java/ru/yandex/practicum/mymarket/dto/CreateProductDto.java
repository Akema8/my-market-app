package ru.yandex.practicum.mymarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateProductDto(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        String imgPath,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        Long price
) {
}