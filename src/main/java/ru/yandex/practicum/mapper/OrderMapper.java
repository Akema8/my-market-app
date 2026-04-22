package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.model.Order;

@Mapper(uses = {OrderItemMapper.class,})
public interface OrderMapper {
    OrderDto toDto(Order order);
    Order toEntity(OrderDto orderDto);
}