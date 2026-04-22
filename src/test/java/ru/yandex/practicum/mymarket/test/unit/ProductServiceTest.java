package ru.yandex.practicum.mymarket.test.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.mapper.ProductMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;
import ru.yandex.practicum.mymarket.service.ProductService;

import java.util.*;

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
        List<Product> products = List.of(product);
        when(productRepository.findAll()).thenReturn(products);

        ProductDto dto = new ProductDto();
        dto.setId(1L);
        dto.setTitle("Product 1");
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        verify(productRepository).findAll();
        verify(productMapper).toDto(any(Product.class));
    }

    @Test
    public void testFindItemsWithoutSearch() {
        Product product = new Product("Title", "Description", "images/test.jpg", 50L);
        product.setId(1L);
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(page);

        ProductDto dto = new ProductDto();
        dto.setId(1L);
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        when(cartItemRepository.findCountsByProductIds(anyList()))
                .thenReturn(Collections.singletonList(new Object[]{1L, 5}));

        Page<ProductDto> result = productService.findItems(null, "NO", 1, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).count()).isEqualTo(5);
        verify(productRepository).findAll(any(PageRequest.class));
        verify(cartItemRepository).findCountsByProductIds(anyList());
    }

    @Test
    public void testChangeItemQuantityPlus() {
        Long productId = 1L;
        Product product = new Product("Title", "Desc", "images/test.jpg", 100L);
        product.setId(productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        CartItem cartItem = new CartItem(product, 2);
        when(cartItemRepository.findByProduct_Id(productId)).thenReturn(Optional.of(cartItem));

        boolean result = productService.changeItemQuantity(productId, "PLUS");

        assertThat(result).isTrue();
        verify(cartItemRepository).save(any(CartItem.class));
        assertThat(cartItem.getCount()).isEqualTo(3);
    }

    @Test
    public void testChangeItemQuantityMinusAndDelete() {
        Long productId = 1L;
        Product product = new Product("Title", "Desc", "", 100L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        CartItem cartItem = new CartItem(product, 1);
        when(cartItemRepository.findByProduct_Id(productId)).thenReturn(Optional.of(cartItem));

        boolean result = productService.changeItemQuantity(productId, "MINUS");

        assertThat(result).isTrue();
        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    public void testGetItemById_withCount() {
        Long id = 1L;
        Product product = new Product("Title", "Desc", "", 100L);
        product.setId(id);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        Object[] countData = new Object[]{id, 7};
        when(cartItemRepository.findCountsByProductIds(Collections.singletonList(id)))
                .thenReturn(Collections.singletonList(countData));

        ProductDto dto = new ProductDto();
        dto.setId(id);
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        ProductDto result = productService.getItemById(id);

        assertThat(result).isNotNull();
        assertThat(result.count()).isEqualTo(7);
        verify(productRepository).findById(id);
        verify(cartItemRepository).findCountsByProductIds(anyList());
    }

    @Test
    public void testGetItemsInCart() {
        Product product = new Product("Title", "Desc", "",100L);
        product.setId(1L);
        CartItem cartItem = new CartItem(product, 3);
        when(cartItemRepository.findAll()).thenReturn(List.of(cartItem));

        ProductDto dto = new ProductDto();
        dto.setId(1L);
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        List<ProductDto> result = productService.getItemsInCart();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).count()).isEqualTo(3);
        verify(cartItemRepository).findAll();
        verify(productMapper).toDto(any(Product.class));
    }
}
