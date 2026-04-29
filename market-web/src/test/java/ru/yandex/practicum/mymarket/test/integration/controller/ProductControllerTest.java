package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ProductDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ProductControllerTest extends BaseControllerTest {

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

    @Test
    public void testChangeItemQuantity_WithSpecialCharactersInSearch() {
        when(productService.changeItemQuantity(anyLong(), anyString()))
                .thenReturn(Mono.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", "1");
        formData.add("action", "PLUS");
        formData.add("search", "test & value=123");
        formData.add("sort", "PRICE");
        formData.add("pageNumber", "2");
        formData.add("pageSize", "10");

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items?search=test%20%26%20value%3D123&sort=PRICE&pageNumber=2&pageSize=10");
    }

    @Test
    public void testChangeItemQuantity_WithCyrillicAndSpecialChars() {
        when(productService.changeItemQuantity(anyLong(), anyString()))
                .thenReturn(Mono.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("id", "2");
        formData.add("action", "MINUS");
        formData.add("search", "товар №1+налог");
        formData.add("sort", "NAME");
        formData.add("pageNumber", "1");
        formData.add("pageSize", "5");

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location ->
                        org.assertj.core.api.Assertions.assertThat(location)
                                .contains("search=")
                                .contains("sort=NAME")
                                .contains("pageNumber=1")
                                .contains("pageSize=5")
                );
    }
}
