package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.mymarket.dto.OrderItemDto;
import ru.yandex.practicum.mymarket.model.OrderItem;

@Mapper(uses = {ProductMapper.class}, componentModel = "spring")
public interface OrderItemMapper {
    @Mapping(source = "product.title", target = "title")
    OrderItemDto toDto(OrderItem orderItem);
    OrderItem toEntity(OrderItemDto orderItemDto);
}