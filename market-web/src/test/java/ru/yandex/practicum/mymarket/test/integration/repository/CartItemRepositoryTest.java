package ru.yandex.practicum.mymarket.test.integration.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CartItemRepositoryTest extends BaseRepositoryTest {

    private static final Long CART_ID = 1L;

    @BeforeEach
    public void setUp() {
        cartItemRepository.deleteAll().block();
        productRepository.deleteAll().block();
    }

    @Test
    public void testFindByProductId() {
        Product product = new Product("Test Product", "Desc", "img.jpg", 100L);
        Long productId = productRepository.save(product).block().getId();

        CartItem cartItem = new CartItem(CART_ID, productId, 3);
        cartItemRepository.save(cartItem).block();

        StepVerifier.create(cartItemRepository.findByProductIdAndCartId(productId, CART_ID))
                .assertNext(found -> {
                    assertThat(found.getProductId()).isEqualTo(productId);
                    assertThat(found.getCount()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    public void testFindByProductIdIn() {
        Product product1 = new Product("Product 1", "Desc1", "img1.jpg", 100L);
        Product product2 = new Product("Product 2", "Desc2", "img2.jpg", 200L);

        Long productId1 = productRepository.save(product1).block().getId();
        Long productId2 = productRepository.save(product2).block().getId();

        cartItemRepository.save(new CartItem(CART_ID, productId1, 2)).block();
        cartItemRepository.save(new CartItem(CART_ID, productId2, 5)).block();
        cartItemRepository.save(new CartItem(CART_ID, productId1, 4)).block();

        StepVerifier.create(cartItemRepository.findByProductIdInAndCartId(List.of(productId1, productId2), CART_ID))
                .expectNextCount(3)
                .verifyComplete();
    }
}
