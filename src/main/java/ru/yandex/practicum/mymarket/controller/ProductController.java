package ru.yandex.practicum.mymarket.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.Paging;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping({"/", "/items"})
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String getProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false, defaultValue = "NO") String sort,
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "5") int pageSize,
            Model model
    ) {

        Page<ProductDto> page = productService.findItems(search, sort, pageNumber, pageSize);
        List<ProductDto> items = page.getContent();

        List<List<ProductDto>> itemRows = new ArrayList<>();
        for (int i = 0; i < items.size(); i += 3) {
            itemRows.add(items.subList(i, Math.min(i + 3, items.size())));
        }

        model.addAttribute("items", itemRows);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", new Paging(pageSize, pageNumber, page.hasPrevious(), page.hasNext()));
        return "items";
    }

    @PostMapping
    public String changeItemQuantity(
            @RequestParam Long id,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            @RequestParam String action
    ) {

        boolean success = productService.changeItemQuantity(id, action);

        return "redirect:/items?search=" + search + "&sort=" + sort +
                "&pageNumber=" + pageNumber + "&pageSize=" + pageSize;
    }

    @GetMapping("/{id}")
    public String getItemPage(@PathVariable Long id, Model model) {

        ProductDto product = productService.getItemById(id);
        model.addAttribute("item", product);
        return "item";
    }

    @PostMapping("/{id}")
    public String updateItemCountInCart(
            @PathVariable Long id,
            @RequestParam String action,
            Model model
    ) {
        boolean success = productService.changeItemQuantity(id, action);
        ProductDto product = productService.getItemById(id);
        model.addAttribute("item", product);
        return "item";
    }

}
