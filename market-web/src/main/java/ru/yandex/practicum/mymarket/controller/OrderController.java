package ru.yandex.practicum.mymarket.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.service.OrderService;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Mono<String> getOrders(Authentication authentication, Model model) {
        return userRepository.findByUsername(authentication.getName())
                .flatMap(user -> orderService.getAllOrders(user.getId()).collectList())
                .map(orders -> {
                    model.addAttribute("orders", orders);
                    return "orders";
                });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrderPage(
            @PathVariable long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Authentication authentication,
            Model model
    ) {
        return userRepository.findByUsername(authentication.getName())
                .flatMap(user -> orderService.getOrderByIdForUser(id, user.getId()))
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                    return "order";
                });
    }
}
