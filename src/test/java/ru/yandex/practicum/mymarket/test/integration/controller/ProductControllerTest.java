package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.controller.ProductController;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProductService productService;

    @Test
    public void testGetProducts() {
        var mockPage = new PageImpl<>(List.of(
                new ProductDto(1L, "Product1", "Desc1", "img1.png", 100L, 2),
                new ProductDto(2L, "Product2", "Desc2", "img2.png", 200L, 0)
        ));
        when(productService.findItems(any(), anyString(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockPage));

        webTestClient.get()
                .uri("/items?search=&sort=NO&pageNumber=1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetProducts_WithParams() {
        var mockPage = new PageImpl<>(List.of(
                new ProductDto(1L, "Product1", "Desc1", "img1.png", 100L, 2),
                new ProductDto(2L, "Product2", "Desc2", "img2.png", 200L, 0),
                new ProductDto(3L, "Product3", "Desc3", "img3.png", 300L, 1)
        ));
        when(productService.findItems(eq("searchTerm"), eq("PRICE"), eq(2), eq(3)))
                .thenReturn(Mono.just(mockPage));

        webTestClient.get()
                .uri("/items?search=searchTerm&sort=PRICE&pageNumber=2&pageSize=3")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetItemPage() {
        var mockProduct = new ProductDto(1L, "Test Product", "Desc", "img.png", 100L, 0);
        when(productService.getItemById(1L)).thenReturn(Mono.just(mockProduct));

        webTestClient.get()
                .uri("/items/1")
                .exchange()
                .expectStatus().isOk();
    }
}
