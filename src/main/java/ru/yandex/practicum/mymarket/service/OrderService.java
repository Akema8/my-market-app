package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.mapper.OrderMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CartItemRepository cartItemRepository;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.cartItemRepository = cartItemRepository;
    }

    public List<OrderDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(orderMapper::toDto)
                .toList();
    }

    public OrderDto getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toDto)
                .orElse(null);
    }

    public long createOrder() {
        List<CartItem> cartItems = cartItemRepository.findAll();

        List<OrderItem> orderItems = cartItems
                .stream()
                .map(cartItem -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setCount(cartItem.getCount());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            return orderItem;
        }).toList();
        Long totalSum = orderItems.stream()
                .mapToLong(oi -> oi.getPrice() * oi.getCount())
                .sum();

        Order order = new Order();
        order.setItems(orderItems);
        order.setTotalSum(totalSum);
        orderItems.forEach(oi -> oi.setOrder(order));
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return savedOrder.getId();
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }
}