package ru.yandex.practicum.mymarket.test.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;
import ru.yandex.practicum.mymarket.service.OrderService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CART_ID = 2L;
    private static final Long PRODUCT_ID = 1L;

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllOrders_ReturnsMappedDtos() {
        Order order1 = new Order(100L, USER_ID);
        order1.setId(1L);
        Order order2 = new Order(200L, USER_ID);
        order2.setId(2L);

        when(orderRepository.findByUserId(USER_ID)).thenReturn(Flux.just(order1, order2));
        when(orderItemRepository.findByOrderId(anyLong())).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getAllOrders(USER_ID))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(1L))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(2L))
                .verifyComplete();
    }

    @Test
    void testGetOrderById_Found() {
        Order order = new Order(50L, USER_ID);
        order.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Mono.just(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(orderService.getOrderById(1L))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.totalSum()).isEqualTo(50L);
                })
                .verifyComplete();
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById(1L))
                .verifyComplete();
    }

    @Test
    void testCreateOrder_Success() {
        Product product = new Product("Title", "Desc", "", 100L);
        product.setId(PRODUCT_ID);

        CartItem cartItem = new CartItem(CART_ID, PRODUCT_ID, 2);
        cartItem.setId(10L);

        Order savedOrder = new Order(200L, USER_ID);
        savedOrder.setId(5L);

        OrderItem savedItem = new OrderItem();
        savedItem.setId(20L);

        when(cartItemRepository.findByCartId(CART_ID)).thenReturn(Flux.just(cartItem));
        when(productRepository.findAllById(anyIterable())).thenReturn(Flux.just(product));
        when(orderRepository.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
        when(orderItemRepository.saveAll(anyIterable())).thenReturn(Flux.just(savedItem));
        when(cartItemRepository.deleteAll(anyIterable())).thenReturn(Mono.empty());

        StepVerifier.create(orderService.createOrder(USER_ID, CART_ID))
                .assertNext(id -> assertThat(id).isEqualTo(5L))
                .verifyComplete();

        verify(cartItemRepository).deleteAll(anyIterable());
    }
}
