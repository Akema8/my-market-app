package ru.yandex.practicum.mymarket.dto;

public record ProductDto(Long id, String title, String description, String imgPath, Long price, int count) {
    public ProductDto withCount(int count) {
        return new ProductDto(id, title, description, imgPath, price, count);
    }
}