package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.model.Order;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findByUserId(Long userId);
}