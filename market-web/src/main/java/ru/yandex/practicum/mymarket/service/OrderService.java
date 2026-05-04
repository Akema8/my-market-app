package ru.yandex.practicum.mymarket.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public Flux<OrderDto> getAllOrders(Long userId) {
        return orderRepository.findByUserId(userId).flatMap(this::assembleOrderDto);
    }

    public Mono<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id).flatMap(this::assembleOrderDto);
    }

    @Transactional
    public Mono<Long> createOrder(Long userId, Long cartId) {
        return cartItemRepository.findByCartId(cartId)
                .collectList()
                .flatMap(cartItems -> {
                    List<Long> productIds = cartItems.stream()
                            .map(CartItem::getProductId)
                            .toList();

                    return productRepository.findAllById(productIds)
                            .collectMap(Product::getId)
                            .flatMap(productMap -> {
                                long totalSum = cartItems.stream()
                                        .mapToLong(ci -> productMap.get(ci.getProductId()).getPrice() * ci.getCount())
                                        .sum();

                                Order order = new Order(totalSum, userId);
                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> {
                                            List<OrderItem> items = cartItems.stream()
                                                    .map(ci -> {
                                                        OrderItem oi = new OrderItem();
                                                        oi.setOrderId(savedOrder.getId());
                                                        oi.setProductId(ci.getProductId());
                                                        oi.setCount(ci.getCount());
                                                        oi.setPrice(productMap.get(ci.getProductId()).getPrice());
                                                        return oi;
                                                    })
                                                    .toList();
                                            return orderItemRepository.saveAll(items)
                                                    .then(cartItemRepository.deleteAll(cartItems))
                                                    .thenReturn(savedOrder.getId());
                                        });
                            });
                });
    }

    public Mono<Void> deleteOrder(Long id) {
        return orderRepository.deleteById(id);
    }

    private Mono<OrderDto> assembleOrderDto(Order order) {
        return orderItemRepository.findByOrderId(order.getId())
                .collectList()
                .flatMap(items -> {
                    List<Long> productIds = items.stream().map(OrderItem::getProductId).toList();
                    if (productIds.isEmpty()) {
                        return Mono.just(new OrderDto(order.getId(), List.of(), order.getTotalSum()));
                    }
                    return productRepository.findAllById(productIds)
                            .collectMap(p -> p.getId())
                            .map((Map<Long, Product> productMap) -> {
                                List<OrderItemDto> itemDtos = items.stream()
                                        .map(oi -> new OrderItemDto(
                                                oi.getId(),
                                                productMap.get(oi.getProductId()).getTitle(),
                                                oi.getCount(),
                                                oi.getPrice()
                                        ))
                                        .toList();
                                return new OrderDto(order.getId(), itemDtos, order.getTotalSum());
                            });
                });
    }
}
