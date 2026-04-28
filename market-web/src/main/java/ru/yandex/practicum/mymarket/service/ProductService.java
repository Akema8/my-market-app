package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.mapper.ProductMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper, CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.cartItemRepository = cartItemRepository;
    }

    public Flux<ProductDto> getAllProducts() {
        return productRepository.findAll().map(productMapper::toDto);
    }

    @Cacheable(
        value = "products",
        key = "(#search ?: 'all') + ':' + #sort + ':' + #pageNumber + ':' + #pageSize",
        unless = "#result == null"
    )
    public Mono<Page<ProductDto>> findItems(String search, String sort, int pageNumber, int pageSize) {
        log.info("Loading from DB: search={}, sort={}, page={}, size={}", search, sort, pageNumber, pageSize);
        return loadFromDatabase(search, sort, pageNumber, pageSize);
    }

    private Mono<Page<ProductDto>> loadFromDatabase(String search, String sort, int pageNumber, int pageSize) {
        Sort sorting = buildSort(sort);
        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, sorting);

        Flux<ru.yandex.practicum.mymarket.model.Product> productFlux;
        Mono<Long> countMono;

        if (search == null || search.isBlank()) {
            productFlux = productRepository.findAllBy(pageable);
            countMono = productRepository.count();
        } else {
            productFlux = productRepository
                    .findByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(search, search, pageable);
            countMono = productRepository
                    .countByTitleContainsIgnoreCaseOrDescriptionContainsIgnoreCase(search, search);
        }

        return productFlux
                .collectList()
                .zipWith(countMono)
                .flatMap(tuple -> {
                    List<ru.yandex.practicum.mymarket.model.Product> products = tuple.getT1();
                    long total = tuple.getT2();
                    List<Long> ids = products.stream()
                            .map(Product::getId)
                            .toList();

                    if (ids.isEmpty()) {
                        return Mono.just((Page<ProductDto>) new PageImpl<ProductDto>(List.of(), pageable, 0));
                    }

                    return cartItemRepository.findByProductIdIn(ids)
                            .collectList()
                            .map(cartItems -> {
                                Map<Long, Integer> counts = cartItems.stream()
                                        .collect(Collectors.groupingBy(
                                                CartItem::getProductId,
                                                Collectors.summingInt(CartItem::getCount)));
                                List<ProductDto> dtos = products.stream()
                                        .map(p -> productMapper.toDto(p)
                                                .withCount(counts.getOrDefault(p.getId(), 0)))
                                        .toList();
                                return (Page<ProductDto>) new PageImpl<>(dtos, pageable, total);
                            });
                });
    }

    @CacheEvict(value = {"products", "product", "cart"}, allEntries = true)
    public Mono<Void> changeItemQuantity(Long productId, String action) {
        log.info("changeItemQuantity called: productId={}, action={}", productId, action);
        return productRepository.findById(productId)
                .doOnNext(p -> log.info("Found product: {}", p.getId()))
                .flatMap(product ->
                        cartItemRepository.findByProductId(productId)
                                .doOnNext(ci -> log.info("Found cartItem: id={}, count={}", ci.getId(), ci.getCount()))
                                .defaultIfEmpty(new CartItem(productId, 0))
                                .doOnNext(ci -> log.info("After defaultIfEmpty: id={}, productId={}, count={}", ci.getId(), ci.getProductId(), ci.getCount()))
                                .flatMap(cartItem -> {
                                    int current = cartItem.getCount();
                                    if ("PLUS".equals(action)) {
                                        cartItem.setCount(current + 1);
                                        log.info("PLUS action: saving cartItem with count={}", cartItem.getCount());
                                        return cartItemRepository.save(cartItem)
                                                .doOnSuccess(saved -> log.info("Saved cartItem: id={}", saved.getId()))
                                                .doOnError(e -> log.error("Error saving cartItem", e))
                                                .then();
                                    } else if ("MINUS".equals(action)) {
                                        if (current > 1) {
                                            cartItem.setCount(current - 1);
                                            log.info("MINUS action: saving cartItem with count={}", cartItem.getCount());
                                            return cartItemRepository.save(cartItem).then();
                                        } else if (cartItem.getId() != null) {
                                            log.info("MINUS action: deleting cartItem");
                                            return cartItemRepository.delete(cartItem);
                                        }
                                    }
                                    else if ("DELETE".equals(action)) {
                                        return cartItemRepository.delete(cartItem);
                                    }

                                    return Mono.empty();
                                })
                )
                .doOnError(e -> log.error("Error in changeItemQuantity", e))
                .then();
    }

    @Cacheable(value = "product", key = "#id", unless = "#result == null")
    public Mono<ProductDto> getItemById(Long id) {
        log.info("Loading product from DB: id={}", id);
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .flatMap(dto ->
                        cartItemRepository.findByProductId(id)
                                .map(ci -> dto.withCount(ci.getCount()))
                                .defaultIfEmpty(dto.withCount(0))
                );
    }

    @Cacheable(value = "cart", key = "'all'", unless = "#result == null")
    public Flux<ProductDto> getItemsInCart() {
        log.info("Loading cart from DB");
        return cartItemRepository.findAll()
                .flatMap(cartItem ->
                        productRepository.findById(cartItem.getProductId())
                                .map(product -> productMapper.toDto(product).withCount(cartItem.getCount()))
                );
    }

    @CacheEvict(value = {"products", "product", "cart"}, allEntries = true)
    public Mono<ProductDto> createItem(String title, String description, String imgPath, Long price) {
        log.info("Creating new product: title={}, price={}", title, price);
        Product product =
                new Product(title, description, imgPath, price);
        return productRepository.save(product)
                .map(productMapper::toDto)
                .map(dto -> dto.withCount(10));
    }

    private Sort buildSort(String sort) {
        return switch (sort) {
            case "ALPHA" -> Sort.by("title").ascending();
            case "PRICE" -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };
    }
}