package ru.yandex.practicum.mymarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.PaymentClient;
import ru.yandex.practicum.mymarket.service.ProductService;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final Long USER_ID = 1L;

    private final OrderService orderService;
    private final PaymentClient paymentClient;
    private final ProductService productService;

    public OrderController(OrderService orderService, PaymentClient paymentClient, ProductService productService) {
        this.orderService = orderService;
        this.paymentClient = paymentClient;
        this.productService = productService;
    }

    @GetMapping
    public Mono<String> getOrders(Model model) {
        return orderService.getAllOrders()
                .collectList()
                .map(orders -> {
                    model.addAttribute("orders", orders);
                    return "orders";
                });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrderPage(
            @PathVariable long id,
            @RequestParam(value = "newOrder", required = false, defaultValue = "false") boolean newOrder,
            Model model
    ) {
        return orderService.getOrderById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                    return "order";
                });
    }
}