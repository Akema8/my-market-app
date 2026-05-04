package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Product;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Long> {
    Flux<Product> findAllBy(Pageable pageable);
    Flux<Product> findByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(
            String title, String description, Pageable pageable);
    Mono<Long> countByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(
            String title, String description);
}