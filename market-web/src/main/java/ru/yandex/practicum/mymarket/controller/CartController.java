package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Long USER_ID = 1L;

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/items")
    public Mono<String> getCartItems(Model model) {
        return cartService.getCartSummary(USER_ID)
                .map(summary -> {
                    model.addAttribute("items", summary.items());
                    model.addAttribute("total", summary.total());
                    model.addAttribute("balance", summary.balance());
                    model.addAttribute("hasSufficientFunds", summary.hasSufficientFunds());
                    model.addAttribute("serviceAvailable", summary.serviceAvailable());
                    return "cart";
                });
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartItemForm form, Model model) {
        return cartService.updateItemAndGetSummary(form.getId(), form.getAction(), USER_ID)
                .map(summary -> {
                    model.addAttribute("items", summary.items());
                    model.addAttribute("total", summary.total());
                    model.addAttribute("balance", summary.balance());
                    model.addAttribute("hasSufficientFunds", summary.hasSufficientFunds());
                    model.addAttribute("serviceAvailable", summary.serviceAvailable());
                    return "cart";
                });
    }
}
