package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Long USER_ID = 1L;

    private final ProductService productService;
    private final PaymentClient paymentClient;

    public CartController(ProductService productService, PaymentClient paymentClient) {
        this.productService = productService;
        this.paymentClient = paymentClient;
    }

    @GetMapping("/items")
    public Mono<String> getCartItems(Model model) {
        return productService.getItemsInCart()
                .collectList()
                .flatMap(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();

                    return paymentClient.getBalance(USER_ID)
                            .defaultIfEmpty(new ru.yandex.practicum.mymarket.dto.BalanceResponse(USER_ID, 0L))
                            .map(balance -> {
                                boolean hasSufficientFunds = balance.balance() >= total;
                                boolean serviceAvailable = balance.balance() > 0 || total == 0;

                                model.addAttribute("items", cartItems);
                                model.addAttribute("total", total);
                                model.addAttribute("balance", balance.balance());
                                model.addAttribute("hasSufficientFunds", hasSufficientFunds);
                                model.addAttribute("serviceAvailable", serviceAvailable);
                                return "cart";
                            });
                });
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartItemForm form, Model model) {
        return productService.changeItemQuantity(form.getId(), form.getAction())
                .then(productService.getItemsInCart().collectList())
                .flatMap(cartItems -> {
                    long total = cartItems.stream()
                            .mapToLong(item -> item.price() * item.count())
                            .sum();

                    return paymentClient.getBalance(USER_ID)
                            .defaultIfEmpty(new ru.yandex.practicum.mymarket.dto.BalanceResponse(USER_ID, 0L))
                            .map(balance -> {
                                boolean hasSufficientFunds = balance.balance() >= total;
                                boolean serviceAvailable = balance.balance() > 0 || total == 0;

                                model.addAttribute("items", cartItems);
                                model.addAttribute("total", total);
                                model.addAttribute("balance", balance.balance());
                                model.addAttribute("hasSufficientFunds", hasSufficientFunds);
                                model.addAttribute("serviceAvailable", serviceAvailable);
                                return "cart";
                            });
                });
    }
}