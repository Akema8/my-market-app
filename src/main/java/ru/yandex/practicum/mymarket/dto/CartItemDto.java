package ru.yandex.practicum.mymarket.dto;

public class CartItemDto {

    private Long id;
    private ProductDto product;
    private int count;

    public CartItemDto() {}

    public CartItemDto(ProductDto product, int count) {
        this.product = product;
        this.count = count;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProductDto getProduct() { return product; }
    public void setProduct(ProductDto product) { this.product = product; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
