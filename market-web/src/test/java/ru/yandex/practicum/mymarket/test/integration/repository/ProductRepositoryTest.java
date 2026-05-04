package ru.yandex.practicum.mymarket.test.integration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.Product;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductRepositoryTest extends BaseRepositoryTest {

    @BeforeEach
    public void setUp() {
        productRepository.deleteAll().block();
    }

    @Test
    public void testFindByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase() {
        Product product1 = new Product("Test Product", "A product for testing", "img1.jpg", 100L);
        Product product2 = new Product("Another Product", "Contains Test", "img2.jpg", 200L);

        productRepository.save(product1).block();
        productRepository.save(product2).block();

        Pageable pageable = PageRequest.of(0, 10);

        StepVerifier.create(
                productRepository.findByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(
                        "test", "test", pageable)
        )
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void testFindAllBy() {
        Product product1 = new Product("Product A", "Description A", "img1.jpg", 100L);
        Product product2 = new Product("Product B", "Description B", "img2.jpg", 200L);
        Product product3 = new Product("Product C", "Description C", "img3.jpg", 300L);

        productRepository.save(product1).block();
        productRepository.save(product2).block();
        productRepository.save(product3).block();

        Pageable pageable = PageRequest.of(0, 2);

        StepVerifier.create(productRepository.findAllBy(pageable))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    public void testCountByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase() {
        Product product1 = new Product("Special Product", "Desc", "img1.jpg", 100L);
        Product product2 = new Product("Another", "Special description", "img2.jpg", 200L);
        Product product3 = new Product("Regular", "Regular desc", "img3.jpg", 150L);

        productRepository.save(product1).block();
        productRepository.save(product2).block();
        productRepository.save(product3).block();

        StepVerifier.create(
                productRepository.countByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(
                        "special", "special")
        )
                .assertNext(count -> assertThat(count).isEqualTo(2L))
                .verifyComplete();
    }
}