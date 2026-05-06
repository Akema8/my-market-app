package ru.yandex.practicum.mymarket.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.CheckoutService;

@Controller
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/buy")
    public Mono<String> checkout(Authentication authentication, WebSession session) {
        return checkoutService.checkout(authentication.getName())
                .map(result -> {
                    if (!result.success()) {
                        session.getAttributes().put("error", result.errorMessage());
                        return "redirect:/cart/items";
                    }
                    session.getAttributes().put("success",
                            "Заказ успешно оформлен! Остаток на балансе: " + result.remainingBalance() + " руб.");
                    return "redirect:/orders/" + result.orderId() + "?newOrder=true";
                });
    }
}
