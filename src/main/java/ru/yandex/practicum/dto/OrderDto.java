package ru.yandex.practicum.dto;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    public Long getTotalSum() { return totalSum; }
    public void setTotalSum(Long totalSum) { this.totalSum = totalSum; }
}
