package ru.yandex.practicum.mymarket.test.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.mapper.ProductMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductMapper productMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAllProducts() {
        Product product = new Product("Product 1", "Desc", "", 100L);
        product.setId(1L);
        ProductDto dto = new ProductDto(1L, "Product 1", null, null, null, 0);

        when(productRepository.findAll()).thenReturn(Flux.just(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        StepVerifier.create(productService.getAllProducts())
                .expectNext(dto)
                .verifyComplete();

        verify(productRepository).findAll();
        verify(productMapper).toDto(any(Product.class));
    }

    @Test
    public void testFindItemsWithoutSearch() {
        Product product = new Product("Title", "Description", "images/test.jpg", 50L);
        product.setId(1L);
        ProductDto dto = new ProductDto(1L, "Title", null, null, 50L, 0);
        CartItem cartItem = new CartItem(1L, 5);

        when(productRepository.findAllBy(any())).thenReturn(Flux.just(product));
        when(productRepository.count()).thenReturn(Mono.just(1L));
        when(cartItemRepository.findByProductIdIn(anyList())).thenReturn(Flux.just(cartItem));
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        StepVerifier.create(productService.findItems(null, "NO", 1, 10))
                .assertNext(page -> {
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().get(0).count()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    public void testChangeItemQuantityPlus() {
        Long productId = 1L;
        Product product = new Product("Title", "Desc", "images/test.jpg", 100L);
        product.setId(productId);
        CartItem cartItem = new CartItem(productId, 2);
        cartItem.setId(10L);

        when(productRepository.findById(productId)).thenReturn(Mono.just(product));
        when(cartItemRepository.findByProductId(productId)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(Mono.just(cartItem));

        StepVerifier.create(productService.changeItemQuantity(productId, "PLUS"))
                .verifyComplete();

        verify(cartItemRepository).save(argThat(ci -> ci.getCount() == 3));
    }

    @Test
    public void testChangeItemQuantityMinusAndDelete() {
        Long productId = 1L;
        Product product = new Product("Title", "Desc", "", 100L);
        product.setId(productId);
        CartItem cartItem = new CartItem(productId, 1);
        cartItem.setId(10L);

        when(productRepository.findById(productId)).thenReturn(Mono.just(product));
        when(cartItemRepository.findByProductId(productId)).thenReturn(Mono.just(cartItem));
        when(cartItemRepository.delete(any(CartItem.class))).thenReturn(Mono.empty());

        StepVerifier.create(productService.changeItemQuantity(productId, "MINUS"))
                .verifyComplete();

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    public void testGetItemById_withCount() {
        Long id = 1L;
        Product product = new Product("Title", "Desc", "", 100L);
        product.setId(id);
        ProductDto dto = new ProductDto(id, "Title", "Desc", "", 100L, 0);
        CartItem cartItem = new CartItem(id, 7);

        when(productRepository.findById(id)).thenReturn(Mono.just(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);
        when(cartItemRepository.findByProductId(id)).thenReturn(Mono.just(cartItem));

        StepVerifier.create(productService.getItemById(id))
                .assertNext(result -> assertThat(result.count()).isEqualTo(7))
                .verifyComplete();
    }

    @Test
    public void testGetItemsInCart() {
        Product product = new Product("Title", "Desc", "", 100L);
        product.setId(1L);
        CartItem cartItem = new CartItem(1L, 3);
        ProductDto dto = new ProductDto(1L, "Title", "Desc", "", 100L, 0);

        when(cartItemRepository.findAll()).thenReturn(Flux.just(cartItem));
        when(productRepository.findById(1L)).thenReturn(Mono.just(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        StepVerifier.create(productService.getItemsInCart())
                .assertNext(result -> assertThat(result.count()).isEqualTo(3))
                .verifyComplete();
    }
}