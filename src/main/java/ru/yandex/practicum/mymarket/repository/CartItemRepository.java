package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.model.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT ci.product.id, SUM(ci.count) FROM CartItem ci WHERE ci.product.id IN :productIds GROUP BY ci.product.id")
    List<Object[]> findCountsByProductIds(@Param("productIds") List<Long> productIds);

    Optional<CartItem> findByProduct_Id(Long productId);
}