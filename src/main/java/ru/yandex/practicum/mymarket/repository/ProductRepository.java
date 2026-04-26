package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.model.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(String title, String description, Pageable pageable);
    @Query("SELECT new ru.yandex.practicum.mymarket.dto.ProductDto(p.id, p.title, p.description, p.imgPath, p.price, COALESCE(ci.count, 0)) " +
            "FROM Product p LEFT JOIN CartItem ci ON p = ci.product")
    List<ProductDto> findAllWithCartCount();
}