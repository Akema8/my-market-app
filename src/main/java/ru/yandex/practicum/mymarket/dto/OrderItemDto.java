package ru.yandex.practicum.mymarket.dto;

import ru.yandex.practicum.mymarket.model.OrderItem;

public class OrderItemDto {
    private Long id;

    private String title;

    private int count;

    private Long price;

    public OrderItemDto() {}

    public OrderItemDto(int count, Long price, String title) {
        this.count = count;
        this.price = price;
        this.title = title;
    }

    public Long id() { return id; }
    public void setId(Long id) { this.id = id; }

    public int count() { return count; }
    public void setCount(int count) { this.count = count; }

    public Long price() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public String title() { return title; }
    public void setTitle(String title) { this.title = title; }


}
