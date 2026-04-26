package ru.yandex.practicum.mymarket.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("cart_items")
public class CartItem {
    @Id
    private Long id;
    private Long productId;
    private int count;

    public CartItem() {}

    public CartItem(Long productId, int count) {
        this.productId = productId;
        this.count = count;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}