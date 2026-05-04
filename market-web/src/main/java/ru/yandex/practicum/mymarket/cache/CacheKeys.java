package ru.yandex.practicum.mymarket.cache;

public final class CacheKeys {

    public static final String CART_KEY = "cart:all";
    public static final String CART_PATTERN = "cart:*";
    public static final String PRODUCTS_PATTERN = "products:*";
    public static final String PRODUCT_PATTERN = "product:*";
    public static final String BALANCE_PATTERN = "balance:*";

    private CacheKeys() {
    }

    public static String products(String search, String sort, int page, int size) {
        String term = (search != null && !search.isBlank()) ? search : "all";
        return "products:" + term + ":" + sort + ":" + page + "-" + size;
    }

    public static String product(Long id) {
        return "product:" + id;
    }

    public static String balance(Long userId) {
        return "balance:" + userId;
    }
}
