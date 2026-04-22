package ru.yandex.practicum.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int count;

    private Long price;

    public OrderItem() {}

    public OrderItem(Product product, int count, Long price, Order order) {
        this.product = product;
        this.count = count;
        this.price = price;
        this.order = order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
}