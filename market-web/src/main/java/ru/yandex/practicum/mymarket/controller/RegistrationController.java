package ru.yandex.practicum.mymarket.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.UserRepository;

@Controller
@RequestMapping("/register")
public class RegistrationController {

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
    public Mono<String> register(@RequestParam String username, @RequestParam String password, Model model) {
        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Логин и пароль не могут быть пустыми");
            model.addAttribute("username", username);
            return Mono.just("register");
        }
        return userRepository.findByUsername(username)
                .flatMap(existing -> {
                    model.addAttribute("error", "Пользователь с таким именем уже существует");
                    model.addAttribute("username", username);
                    return Mono.just("register");
                })
                .switchIfEmpty(
                        userRepository.save(new User(null, username, passwordEncoder.encode(password), true))
                                .thenReturn("redirect:/login?registered")
                );
    }
}
