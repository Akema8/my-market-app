package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final ProductService productService;

    public CartController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/items")
    public String getCartItems(Model model) {
        List<ProductDto> cartItems = productService.getItemsInCart();
        long totalPrice = cartItems.stream()
                .mapToLong(item -> item.price() * item.count())
                .sum();

        model.addAttribute("items", cartItems);
        model.addAttribute("total", totalPrice);

        return "cart";
    }

    @PostMapping("/items")
    public String updateCartItem(@RequestParam("id") Long productId,
                                 @RequestParam("action") String action,
                                 Model model) {

        productService.changeItemQuantity(productId, action);
        List<ProductDto> cartItems = productService.getItemsInCart();


        long totalPrice = cartItems.stream()
                .mapToLong(item -> item.price() * item.count())
                .sum();

        model.addAttribute("items", cartItems);
        model.addAttribute("total", totalPrice);

        return "cart";
    }

}