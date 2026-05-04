package ru.yandex.practicum.mymarket.dto;

public class CartItemForm {
    private Long id;
    private String action;

    public CartItemForm() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}