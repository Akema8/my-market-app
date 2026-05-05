package ru.yandex.practicum.mymarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String showForm() {
        return "register";
    }

    @PostMapping
    public Mono<String> register(ServerWebExchange exchange, Model model) {
        return exchange.getFormData().flatMap(form -> {
            String username = form.getFirst("username");
            String password = form.getFirst("password");
            log.info("POST /register received: username='{}', password.length={}",
                    username, password == null ? null : password.length());

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                model.addAttribute("error", "Логин и пароль не могут быть пустыми");
                model.addAttribute("username", username);
                return Mono.just("register");
            }
            return userRepository.findByUsername(username)
                    .flatMap(existing -> {
                        log.info("Registration rejected: user '{}' already exists", username);
                        model.addAttribute("error", "Пользователь с таким именем уже существует");
                        model.addAttribute("username", username);
                        return Mono.just("register");
                    })
                    .switchIfEmpty(Mono.defer(() ->
                            userRepository.save(new User(null, username, passwordEncoder.encode(password), true))
                                    .doOnNext(saved -> log.info("Registered new user id={} username='{}'", saved.getId(), saved.getUsername()))
                                    .thenReturn("redirect:/login?registered")
                    ));
        });
    }
}
