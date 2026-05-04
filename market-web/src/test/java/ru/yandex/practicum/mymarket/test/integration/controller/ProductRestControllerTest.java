package ru.yandex.practicum.mymarket.test.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.test.integration.BaseIntegrationTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ProductRestControllerTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCreateItem() {
        String requestBody = """
                {
                    "title": "Новый товар",
                    "description": "Описание нового товара",
                    "imgPath": "/images/new-product.jpg",
                    "price": 1500
                }
                """;

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.title").isEqualTo("Новый товар")
                .jsonPath("$.description").isEqualTo("Описание нового товара")
                .jsonPath("$.imgPath").isEqualTo("/images/new-product.jpg")
                .jsonPath("$.price").isEqualTo(1500)
                .jsonPath("$.count").isEqualTo(0);
    }

    @Test
    void testCreateProduct_ValidationError_MissingTitle() {
        String requestBody = """
                {
                    "description": "Описание",
                    "price": 1000
                }
                """;

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testCreateProduct_ValidationError_NegativePrice() {
        String requestBody = """
                {
                    "title": "Товар",
                    "description": "Описание",
                    "price": -100
                }
                """;

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testCreateProduct_ValidationError_NullPrice() {
        String requestBody = """
                {
                    "title": "Товар",
                    "description": "Описание"
                }
                """;

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }
}