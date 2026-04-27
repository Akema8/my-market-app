package ru.yandex.practicum.mymarket.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.CartItemForm;
import ru.yandex.practicum.mymarket.dto.ChangeQuantityForm;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping({"/", "/items"})
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Mono<String> getProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false, defaultValue = "NO") String sort,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") int pageSize,
            Model model
    ) {
        return productService.findItems(search, sort, pageNumber, pageSize)
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
                });
    }

    @PostMapping
    public Mono<String> changeItemQuantity(@ModelAttribute ChangeQuantityForm form) {
        log.info("POST /items - id={}, action={}, search={}, sort={}, pageNumber={}, pageSize={}",
                form.getId(), form.getAction(), form.getSearch(), form.getSort(),
                form.getPageNumber(), form.getPageSize());

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

        return productService.changeItemQuantity(form.getId(), form.getAction())
                .thenReturn("redirect:" + redirectUrl);
    }

    @GetMapping("/{id}")
    public Mono<String> getItemPage(@PathVariable Long id, Model model) {
        return productService.getItemById(id)
                .map(product -> {
                    model.addAttribute("item", product);
                    return "item";
                });
    }

    @PostMapping("/{id}")
    public Mono<String> updateItemCountInCart(
            @PathVariable("id") Long id,
            @ModelAttribute CartItemForm form,
            Model model
    ) {
        log.info("POST /items/{} - action={}", id, form.getAction());
        return productService.changeItemQuantity(id, form.getAction())
                .then(productService.getItemById(id))
                .map(product -> {
                    model.addAttribute("item", product);
                    return "item";
                });
    }
}