package ru.yandex.practicum.mymarket.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.service.CartService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/items")
    public Mono<String> getCartItems(Authentication authentication, Model model) {
        return cartService.getCartSummary(authentication.getName())
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
    public Mono<String> updateCartItem(@ModelAttribute CartItemForm form, Authentication authentication, Model model) {
        return cartService.updateItemAndGetSummary(form.getId(), form.getAction(), authentication.getName())
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
