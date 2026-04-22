package ru.yandex.practicum.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    private Long totalSum;

    public Order() {}

    public Order(List<OrderItem> items, Long totalSum) {
        this.items = items;
        this.totalSum = totalSum;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public Long getTotalSum() { return totalSum; }
    public void setTotalSum(Long totalSum) { this.totalSum = totalSum; }
}