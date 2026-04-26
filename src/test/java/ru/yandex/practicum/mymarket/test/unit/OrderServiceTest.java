package ru.yandex.practicum.mymarket.test.unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.service.OrderService;

public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllOrders_ReturnsMappedDtos() {
        Order order1 = new Order();
        order1.setId(1L);
        Order order2 = new Order();
        order2.setId(2L);
        List<Order> orders = Arrays.asList(order1, order2);

        OrderDto dto1 = new OrderDto(null, null, null);
        OrderDto dto2 = new OrderDto(null, null, null);

        when(orderRepository.findAll()).thenReturn(orders);
        when(orderMapper.toDto(order1)).thenReturn(dto1);
        when(orderMapper.toDto(order2)).thenReturn(dto2);

        List<OrderDto> result = orderService.getAllOrders();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(dto1, result.get(0));
        assertEquals(dto2, result.get(1));
    }

    @Test
    void testGetOrderById_Found() {
        // Arrange
        Long id = 1L;
        Order order = new Order();
        order.setId(id);
        OrderDto dto = new OrderDto(null, null, null);

        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);

        OrderDto result = orderService.getOrderById(id);

        assertNotNull(result);
        assertEquals(dto, result);
    }

    @Test
    void testGetOrderById_NotFound() {
        Long id = 1L;
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        OrderDto result = orderService.getOrderById(id);
        assertNull(result);
    }

    @Test
    void testCreateOrder_Success() {
        Product product = new Product();
        product.setPrice(100L);

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setCount(2);

        List<CartItem> cartItems = Collections.singletonList(cartItem);

        Order order = new Order();
        order.setId(1L);
        order.setItems(Collections.emptyList());
        order.setTotalSum(200L);

        when(cartItemRepository.findAll()).thenReturn(cartItems);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setId(1L);
            return savedOrder;
        });

        Long orderId = orderService.createOrder();

        assertNotNull(orderId);
        assertEquals(1L, orderId);
        verify(cartItemRepository).deleteAll(cartItems);
    }
}
