package ru.yandex.practicum.dto;

public class OrderItemDto {
    private Long id;

    private OrderDto order;

    private ProductDto product;

    private int count;

    private Long price;

    public OrderItemDto() {}

    public OrderItemDto(ProductDto product, int count, Long price, OrderDto order) {
        this.product = product;
        this.count = count;
        this.price = price;
        this.order = order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderDto getOrder() { return order; }
    public void setOrder(OrderDto order) { this.order = order; }

    public ProductDto getProduct() { return product; }
    public void setProduct(ProductDto product) { this.product = product; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
}
