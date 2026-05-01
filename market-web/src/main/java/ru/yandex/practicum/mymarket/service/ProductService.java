package ru.yandex.practicum.mymarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.cache.CacheKeys;
import ru.yandex.practicum.mymarket.dto.CachedPage;
import ru.yandex.practicum.mymarket.dto.ProductDto;
import ru.yandex.practicum.mymarket.mapper.ProductMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Product;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ProductRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductMapper productMapper;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final Duration cacheTtl;

    public ProductService(ProductRepository productRepository,
                          ProductMapper productMapper,
                          CartItemRepository cartItemRepository,
                          ReactiveRedisTemplate<String, Object> redisTemplate,
                          @Qualifier("productsCacheTtl") Duration cacheTtl) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.cartItemRepository = cartItemRepository;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = cacheTtl;
    }

    public Flux<ProductDto> getAllProducts() {
        return productRepository.findAll().map(productMapper::toDto);
    }

    @SuppressWarnings("unchecked")
    public Mono<Page<ProductDto>> findItems(String search, String sort, int pageNumber, int pageSize) {
        String cacheKey = CacheKeys.products(search, sort, pageNumber, pageSize);

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(CachedPage.class)
                .map(this::toPage)
                .doOnNext(cached -> log.info("Cache key: {}", cacheKey))
                .switchIfEmpty(
                    loadFromDatabase(search, sort, pageNumber, pageSize)
                        .doOnNext(page -> log.info("Cache not found, loading from DB: {}", cacheKey))
                        .flatMap(page ->
                            redisTemplate.opsForValue()
                                .set(cacheKey, toCachedPage(page), cacheTtl)
                                .thenReturn(page)
                        )
                );
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
                                    } else if ("DELETE".equals(action)) {
                                        return cartItemRepository.delete(cartItem);
                                    }
                                    return Mono.empty();
                                })
                )
                .doOnError(e -> log.error("Error in changeItemQuantity", e))
                .then()
                .then(updateProductAndCartCache(productId));
    }

    public Mono<ProductDto> getItemById(Long id) {
        String cacheKey = CacheKeys.product(id);

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(ProductDto.class)
                .doOnNext(cached -> log.info("Cache key: {}", cacheKey))
                .switchIfEmpty(
                    productRepository.findById(id)
                        .doOnNext(p -> log.info("Cache not found, loading product from DB: id={}", id))
                        .map(productMapper::toDto)
                        .flatMap(dto ->
                            cartItemRepository.findByProductId(id)
                                .map(ci -> dto.withCount(ci.getCount()))
                                .defaultIfEmpty(dto.withCount(0))
                        )
                        .flatMap(dto ->
                            redisTemplate.opsForValue()
                                .set(cacheKey, dto, cacheTtl)
                                .thenReturn(dto)
                        )
                );
    }

    @SuppressWarnings("unchecked")
    public Flux<ProductDto> getItemsInCart() {
        String cacheKey = CacheKeys.CART_KEY;

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .cast(List.class)
                .doOnNext(cached -> log.info("Cache key: {}", cacheKey))
                .flatMapMany(list -> Flux.fromIterable(list).map(item -> (ProductDto) item))
                .switchIfEmpty(
                    cartItemRepository.findAll()
                        .doOnSubscribe(s -> log.info("Cache not found, loading cart from DB"))
                        .flatMap(cartItem ->
                            productRepository.findById(cartItem.getProductId())
                                    .map(productMapper::toDto)
                                    .map(dto -> dto.withCount(cartItem.getCount()))
                        )
                        .collectList()
                        .flatMap(list ->
                            redisTemplate.opsForValue()
                                .set(cacheKey, list, cacheTtl)
                                .thenReturn(list)
                        )
                        .flatMapMany(Flux::fromIterable)
                );
    }

    public Mono<ProductDto> createItem(String title, String description, String imgPath, Long price) {
        log.info("Creating new product: title={}, price={}", title, price);
        Product product = new Product(title, description, imgPath, price);
        return productRepository.save(product)
                .map(productMapper::toDto)
                .map(dto -> dto.withCount(0))
                .flatMap(dto -> invalidateAllCaches().thenReturn(dto));
    }

    public Mono<Void> clearCart() {
        log.info("Clearing cart");
        return cartItemRepository.deleteAll()
                .then(redisTemplate.keys(CacheKeys.CART_PATTERN)
                        .flatMap(redisTemplate::delete)
                        .then())
                .then(redisTemplate.keys(CacheKeys.PRODUCTS_PATTERN)
                        .flatMap(redisTemplate::delete)
                        .then())
                .doOnSuccess(v -> log.info("Cart cleared successfully"));
    }

    private Mono<Void> invalidateAllCaches() {
        log.info("Invalidating all caches");
        return Flux.concat(
                redisTemplate.keys(CacheKeys.PRODUCTS_PATTERN).flatMap(redisTemplate::delete),
                redisTemplate.keys(CacheKeys.PRODUCT_PATTERN).flatMap(redisTemplate::delete),
                redisTemplate.keys(CacheKeys.CART_PATTERN).flatMap(redisTemplate::delete)
        ).then();
    }

    private Mono<Void> updateProductAndCartCache(Long productId) {
        log.info("Updating cache for product {} and cart", productId);
        Mono<Void> updateProduct = productRepository.findById(productId)
                .map(productMapper::toDto)
                .flatMap(dto ->
                        cartItemRepository.findByProductId(productId)
                                .map(ci -> dto.withCount(ci.getCount()))
                                .defaultIfEmpty(dto.withCount(0))
                )
                .flatMap(updatedDto -> {
                    log.info("Updating product cache: {} count={}", productId, updatedDto.count());
                    return redisTemplate.opsForValue()
                            .set(CacheKeys.product(productId), updatedDto, cacheTtl)
                            .then();
                })
                .onErrorResume(e -> {
                    log.warn("Failed to update product cache for {}, deleting instead", productId, e);
                    return redisTemplate.delete(CacheKeys.product(productId)).then();
                });

        Mono<Void> updateCart = cartItemRepository.findAll()
                .flatMap(cartItem ->
                        productRepository.findById(cartItem.getProductId())
                                .map(product -> productMapper.toDto(product).withCount(cartItem.getCount()))
                )
                .collectList()
                .flatMap(cartList -> {
                    log.info("Updating cart cache: {} items", cartList.size());
                    if (cartList.isEmpty()) {
                        return redisTemplate.keys(CacheKeys.CART_PATTERN).flatMap(redisTemplate::delete).then();
                    } else {
                        return redisTemplate.opsForValue()
                                .set(CacheKeys.CART_KEY, cartList, cacheTtl)
                                .then();
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Failed to update cart cache, deleting instead", e);
                    return redisTemplate.keys(CacheKeys.CART_PATTERN).flatMap(redisTemplate::delete).then();
                });

        Mono<Void> invalidateProductLists = redisTemplate.keys(CacheKeys.PRODUCTS_PATTERN)
                .flatMap(redisTemplate::delete)
                .then()
                .doOnSuccess(v -> log.info("Invalidated product list caches"));

        return Mono.when(updateProduct, updateCart, invalidateProductLists);
    }

    @SuppressWarnings("unchecked")
    private CachedPage<ProductDto> toCachedPage(Page<ProductDto> page) {
        return new CachedPage<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.hasPrevious(),
                page.hasNext()
        );
    }

    @SuppressWarnings("unchecked")
    private Page<ProductDto> toPage(CachedPage cachedPage) {
        List<ProductDto> content = (List<ProductDto>) cachedPage.getContent();
        PageRequest pageRequest = PageRequest.of(cachedPage.getNumber(), cachedPage.getSize());
        return new PageImpl<>(content, pageRequest, cachedPage.getTotalElements());
    }

    private Sort buildSort(String sort) {
        return switch (sort) {
            case "ALPHA" -> Sort.by("title").ascending();
            case "PRICE" -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };
    }
}
