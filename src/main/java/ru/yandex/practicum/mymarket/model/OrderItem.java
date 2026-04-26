package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_items")
public class OrderItem {
    @Id
    private Long id;
    private Long orderId;
    private Long productId;
    private int count;
    private Long price;

    public OrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
}