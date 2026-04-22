package ru.yandex.practicum.mymarket.dto;

import java.util.List;

public class OrderDto {
    private Long id;

    private List<OrderItemDto> items;

    private Long totalSum;

    public OrderDto() {}

    public OrderDto(List<OrderItemDto> items, Long totalSum) {
        this.items = items;
        this.totalSum = totalSum;
    }

    public Long id() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<OrderItemDto> items() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public Long totalSum() { return totalSum; }
    public void setTotalSum(Long totalSum) { this.totalSum = totalSum; }
}
