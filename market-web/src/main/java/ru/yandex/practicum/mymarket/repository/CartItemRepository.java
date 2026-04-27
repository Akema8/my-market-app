package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.CartItem;

import java.util.List;

@Repository
public interface CartItemRepository extends R2dbcRepository<CartItem, Long> {
    Mono<CartItem> findByProductId(Long productId);
    Flux<CartItem> findByProductIdIn(List<Long> productIds);
}