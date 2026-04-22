package ru.yandex.practicum.mymarket.test.integration.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class CartItemRepositoryTest {
    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    public void testFindByProductId() {
        Product product = new Product();
        product.setTitle("Test Product");
        productRepository.save(product);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setCount(3);
        cartItemRepository.save(cartItem);

        // вызов метода
        Optional<CartItem> found = cartItemRepository.findByProduct_Id(product.getId());

        // проверки
        assertThat(found).isPresent();
        assertThat(found.get().getProduct().getId()).isEqualTo(product.getId());
        assertThat(found.get().getCount()).isEqualTo(3);
    }

    @Test
    public void testFindCountsByProductIds() {
        Product product1 = new Product();
        product1.setTitle("Product 1");
        productRepository.save(product1);

        Product product2 = new Product();
        product2.setTitle("Product 2");
        productRepository.save(product2);

        CartItem item1 = new CartItem();
        item1.setProduct(product1);
        item1.setCount(2);
        cartItemRepository.save(item1);

        CartItem item2 = new CartItem();
        item2.setProduct(product2);
        item2.setCount(5);
        cartItemRepository.save(item2);

        CartItem item3 = new CartItem();
        item3.setProduct(product1);
        item3.setCount(4);
        cartItemRepository.save(item3);

        List<Object[]> results = cartItemRepository.findCountsByProductIds(
                Arrays.asList(product1.getId(), product2.getId())
        );

        assertThat(results).hasSize(2);

        for (Object[] row : results) {
            Long productId = (Long) row[0];
            Long totalCount = (Long) row[1];

            if (productId.equals(product1.getId())) {
                assertThat(totalCount).isEqualTo(6); // 2 + 4
            } else if (productId.equals(product2.getId())) {
                assertThat(totalCount).isEqualTo(5);
            }
        }
    }
}
