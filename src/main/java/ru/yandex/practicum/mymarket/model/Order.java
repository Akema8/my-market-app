package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
public class Order {
    @Id
    private Long id;
    private Long totalSum;

    public Order() {}

    public Order(Long totalSum) {
        this.totalSum = totalSum;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTotalSum() { return totalSum; }
    public void setTotalSum(Long totalSum) { this.totalSum = totalSum; }
}