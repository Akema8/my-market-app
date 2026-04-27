package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequestMapping
public class BaseController {

    private final OrderService orderService;

    public BaseController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/buy")
    public Mono<String> buyOrder() {
        return orderService.createOrder()
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}