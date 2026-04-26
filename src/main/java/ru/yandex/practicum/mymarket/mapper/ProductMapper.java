package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.model.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "count", ignore = true)
    ProductDto toDto(Product entity);
    Product toEntity(ProductDto dto);
}
