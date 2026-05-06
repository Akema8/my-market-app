package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ServerWebExchange;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(ServerWebExchange exchange, Model model) {
        var params = exchange.getRequest().getQueryParams();
        model.addAttribute("hasError", params.containsKey("error"));
        model.addAttribute("hasLogout", params.containsKey("logout"));
        model.addAttribute("hasRegistered", params.containsKey("registered"));
        return "login";
    }
}
