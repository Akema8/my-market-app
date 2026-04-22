package ru.yandex.practicum.mymarket.test.integration.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
public class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    public void testFindByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase() {
        Product product1 = new Product();
        product1.setTitle("Test Product");
        product1.setDescription("A product for testing");
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setTitle("Another Product");
        product2.setDescription("Contains Test");
        productRepository.save(product2);

        Pageable pageable = PageRequest.of(0, 10);
        var results = productRepository.findByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase("test", "test", pageable);

        assertThat(results).hasSize(2);
        assertThat(results.getContent()).extracting("title").containsExactlyInAnyOrder("Test Product", "Another Product");
    }

    @Test
    public void testFindAllWithCartCount() {
        Product product = new Product();
        product.setTitle("Product with Cart");
        product.setDescription("Description");
        productRepository.save(product);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setCount(3);
        cartItemRepository.save(cartItem);

        List<ProductDto> dtos = productRepository.findAllWithCartCount();
        assertThat(dtos).anyMatch(dto -> dto.id().equals(product.getId()) && dto.count() == 3);
    }
}
