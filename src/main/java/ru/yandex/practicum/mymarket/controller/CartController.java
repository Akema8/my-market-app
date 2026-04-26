package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.service.ProductService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final ProductService productService;

    public CartController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/items")
    public Mono<String> getCartItems(Model model) {
        return productService.getItemsInCart()
                .collectList()
                .map(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();
                    model.addAttribute("items", cartItems);
                    model.addAttribute("total", total);
                    return "cart";
                });
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartItemForm form, Model model) {
        return productService.changeItemQuantity(form.getId(), form.getAction())
                .then(productService.getItemsInCart().collectList())
                .map(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();
                    model.addAttribute("items", cartItems);
                    model.addAttribute("total", total);
                    return "cart";
                });
    }
}