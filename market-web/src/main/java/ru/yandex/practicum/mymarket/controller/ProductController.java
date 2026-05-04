package ru.yandex.practicum.mymarket.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.dto.ChangeQuantityForm;
import ru.yandex.practicum.mymarket.dto.CreateProductDto;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping({"/", "/items"})
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final CartService cartService;

    public ProductController(ProductService productService, CartService cartService) {
        this.productService = productService;
        this.cartService = cartService;
    }

    @GetMapping
    public Mono<String> getProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false, defaultValue = "NO") String sort,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") int pageSize,
            Authentication authentication,
            Model model
    ) {
        Mono<Long> cartIdMono = resolveCartId(authentication);

        return cartIdMono.flatMap(cartId ->
                productService.findItems(search, sort, pageNumber, pageSize, cartId == 0 ? null : cartId)
                        .map(page -> {
                            List<ProductDto> items = page.getContent();
                            List<List<ProductDto>> itemRows = new ArrayList<>();
                            for (int i = 0; i < items.size(); i += 3) {
                                itemRows.add(items.subList(i, Math.min(i + 3, items.size())));
                            }
                            model.addAttribute("items", itemRows);
                            model.addAttribute("search", search);
                            model.addAttribute("sort", sort);
                            model.addAttribute("paging", new Paging(pageSize, pageNumber,
                                    page.hasPrevious(), page.hasNext()));
                            return "items";
                        })
        );
    }

    @PostMapping
    public Mono<String> changeItemQuantity(@ModelAttribute ChangeQuantityForm form, Authentication authentication) {
        log.info("POST /items changeItemQuantity - id={}, action={}", form.getId(), form.getAction());

        String search = form.getSearch() != null ? form.getSearch() : "";
        String sort = form.getSort() != null ? form.getSort() : "NO";
        Integer pageNumber = form.getPageNumber() != null ? form.getPageNumber() : 1;
        Integer pageSize = form.getPageSize() != null ? form.getPageSize() : 5;

        String redirectUrl = UriComponentsBuilder.fromPath("/items")
                .queryParam("search", search)
                .queryParam("sort", sort)
                .queryParam("pageNumber", pageNumber)
                .queryParam("pageSize", pageSize)
                .encode()
                .build()
                .toUriString();

        return cartService.findOrCreateCart(authentication.getName())
                .flatMap(cart -> productService.changeItemQuantity(form.getId(), form.getAction(), cart.getId()))
                .thenReturn("redirect:" + redirectUrl);
    }

    @GetMapping("/{id}")
    public Mono<String> getItemPage(@PathVariable Long id, Authentication authentication, Model model) {
        Mono<Long> cartIdMono = resolveCartId(authentication);

        return cartIdMono.flatMap(cartId ->
                productService.getItemById(id, cartId == 0 ? null : cartId)
                        .map(product -> {
                            model.addAttribute("item", product);
                            return "item";
                        })
        );
    }

    @PostMapping("/{id}")
    public Mono<String> updateItemCountInCart(
            @PathVariable("id") Long id,
            @ModelAttribute CartItemForm form,
            Authentication authentication,
            Model model
    ) {
        log.info("POST /items/{} - action={}", id, form.getAction());
        return cartService.findOrCreateCart(authentication.getName())
                .flatMap(cart ->
                        productService.changeItemQuantity(id, form.getAction(), cart.getId())
                                .then(productService.getItemById(id, cart.getId()))
                )
                .map(product -> {
                    model.addAttribute("item", product);
                    return "item";
                });
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductDto> createItem(@Valid @RequestBody CreateProductDto request) {
        log.info("POST /items createItem: {}", request.title());
        return productService.createItem(
                request.title(),
                request.description(),
                request.imgPath(),
                request.price()
        );
    }

    private Mono<Long> resolveCartId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return Mono.just(0L);
        }
        return cartService.findOrCreateCart(authentication.getName())
                .map(cart -> cart.getId());
    }
}
