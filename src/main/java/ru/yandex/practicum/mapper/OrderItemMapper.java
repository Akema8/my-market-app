package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.dto.OrderItemDto;
import ru.yandex.practicum.model.OrderItem;

@Mapper(uses = {ProductMapper.class})
public interface OrderItemMapper {
    OrderItemDto toDto(OrderItem orderItem);
    OrderItem toEntity(OrderItemDto orderItemDto);
}