package ru.yandex.practicum.mymarket.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.mapper.ProductMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, CartItemRepository cartItemRepository) {

        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.cartItemRepository = cartItemRepository;
    }

    public List<ProductDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    public Page<ProductDto> findItems(String search, String sort, int pageNumber, int pageSize) {
        Sort sorting;
        switch (sort) {
            case "ALPHA":
                sorting = Sort.by("title").ascending();
                break;
            case "PRICE":
                sorting = Sort.by("price").ascending();
                break;
            case "NO":
            default:
                sorting = Sort.unsorted();
                break;
        }

        PageRequest pageRequest = PageRequest.of(pageNumber - 1, pageSize, sorting);
        Page<Product> productsPage;
        if (search == null || search.trim().isEmpty()) {
            productsPage = productRepository.findAll(pageRequest);
        } else {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            productsPage = productRepository.findByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(search, search, pageRequest);
        }
        List<Long> productIds = productsPage
                .stream()
                .map(Product::getId)
                .toList();

        Map<Long, Integer> productCounts = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Object[]> counts = cartItemRepository.findCountsByProductIds(productIds);
            for (Object[] row : counts) {
                Long productId = (Long) row[0];
                Integer count = ((Number) row[1]).intValue();
                productCounts.put(productId, count);
            }
        }
        Page<ProductDto> dtoPage = productsPage.map(product -> {
            ProductDto dto = productMapper.toDto(product);
            dto.setCount(productCounts.getOrDefault(product.getId(), 0));
            return dto;
        });

        return dtoPage;
    }

    public boolean changeItemQuantity(Long productId, String action) {
        Optional<Product> itemOpt = productRepository.findById(productId);
        if (!itemOpt.isPresent()) {
            return false;
        }
        Product item = itemOpt.get();

        CartItem cartItem = cartItemRepository.findByProduct_Id(productId)
                .orElseGet(() -> new CartItem(item, 0));

        int currentQuantity = cartItem.getCount();

        if (action.equals("PLUS")) {
            cartItem.setCount(currentQuantity + 1);
        } else if (action.equals("MINUS")) {
            if (currentQuantity > 1) {
                cartItem.setCount(currentQuantity - 1);
            } else {
                cartItemRepository.delete(cartItem);
                return true;
            }
        } else {
            return false;
        }
        cartItemRepository.save(cartItem);
        return true;
    }

    public ProductDto getItemById(Long id) {
        ProductDto product = productRepository.findById(id)
                .map(productMapper::toDto)
                .orElse(null);
        ;

        if (product == null) return null;
        List<Object[]> counts = cartItemRepository.findCountsByProductIds(Collections.singletonList(id));
        if (!counts.isEmpty()) {
            Object[] row = counts.get(0);
            Long productId = (Long) row[0];
            Integer count = ((Number) row[1]).intValue();
            product.setCount(count);
        } else {
            product.setCount(0);
        }
        return product;
    }

    public List<ProductDto> getItemsInCart() {
        List<CartItem> cartItems = cartItemRepository.findAll();
        List<ProductDto> productsInCart = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            ProductDto dto = productMapper.toDto(product);
            dto.setCount(cartItem.getCount());
            productsInCart.add(dto);
        }

        return productsInCart;
    }
}