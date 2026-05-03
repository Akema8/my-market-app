# Testing Documentation

Полная документация по тестированию проекта my-market-app.

## Test Coverage Summary

| Модуль | Тестов | Unit | Integration | Context | Статус |
|--------|--------|------|-------------|---------|--------|
| **market-web** | 63 | 28 | 34 | 1 | ✅ Passing |
| **payment-service** | 19 | 8 | 10 | 1 | ✅ Passing |
| **Total** | **82** | **36** | **44** | **2** | ✅ **100%** |

## Quick Start

### Запуск всех тестов
```bash
./mvnw test
```

### Запуск тестов конкретного модуля
```bash
./mvnw test -pl market-web
./mvnw test -pl payment-service
```

### Запуск конкретного теста
```bash
./mvnw test -pl market-web -Dtest=ProductServiceTest
./mvnw test -pl payment-service -Dtest=PaymentServiceTest
```

## market-web (63 tests)

### Unit Tests (28)

**ProductServiceTest (6 tests)**
- ✅ Получение всех товаров
- ✅ Поиск товаров с пагинацией
- ✅ Изменение количества в корзине (PLUS)
- ✅ Изменение количества в корзине (MINUS + delete)
- ✅ Получение товара по ID с количеством в корзине
- ✅ Получение всех товаров в корзине

**OrderServiceTest (4 tests)**
- ✅ Получение всех заказов
- ✅ Получение заказа по ID (найден)
- ✅ Получение заказа по ID (не найден)
- ✅ Создание заказа

**CartServiceTest (5 tests)**
- ✅ Получение сводки корзины — баланс из кэша (PaymentClient не вызывается)
- ✅ Промах кэша — запрос баланса и сохранение в Redis
- ✅ Сервис недоступен — fallback-баланс 0
- ✅ Пустая корзина — достаточно средств при нулевом балансе
- ✅ Инвалидация кэша баланса (evictBalanceCache)

**PaymentClientTest (9 tests)**
- ✅ getBalance — успешный ответ
- ✅ getBalance — сетевая ошибка → empty
- ✅ getBalance — таймаут → empty
- ✅ getBalance — HTTP 500 → empty
- ✅ getBalance — Circuit Breaker открыт → fast fail empty
- ✅ processPayment — успешный ответ
- ✅ processPayment — сетевая ошибка → fallback
- ✅ processPayment — HTTP 500 → fallback
- ✅ processPayment — Circuit Breaker открыт → fallback немедленно

**CheckoutServiceTest (4 tests)**
- ✅ Пустая корзина — сразу failure, без обращения к PaymentClient
- ✅ Платёж отклонён — возвращает сообщение об ошибке
- ✅ Сервис платежей недоступен — fallback failure
- ✅ Успешная оплата — создаёт заказ, очищает корзину, инвалидирует кэш

**Tools:** JUnit 5, Mockito, Reactor Test (StepVerifier), AssertJ

### Integration Tests (34)

**Controller Tests (17 tests)**

*OrderControllerTest (2 tests)*
- ✅ GET /orders — список заказов
- ✅ GET /orders/{id} — страница заказа

*ProductControllerTest (5 tests)*
- ✅ GET /items — витрина товаров
- ✅ GET /items с параметрами поиска
- ✅ GET /items/{id} — страница товара
- ✅ POST /items — изменение количества (спецсимволы в поиске)
- ✅ POST /items — изменение количества (кириллица)

*CartControllerTest (6 tests)*
- ✅ GET /cart/items — пустая корзина
- ✅ GET /cart/items — с товарами
- ✅ GET /cart/items — сервис недоступен
- ✅ POST /cart/items — increment
- ✅ POST /cart/items — decrement
- ✅ POST /cart/items — недостаточно средств

*CheckoutControllerTest (4 tests)*
- ✅ POST /buy — успех → редирект на /orders/{id}
- ✅ POST /buy — пустая корзина → редирект на /cart/items
- ✅ POST /buy — платёж отклонён → редирект на /cart/items
- ✅ POST /buy — сервис недоступен → редирект на /cart/items

**REST API Tests (4 tests)**

*ProductRestControllerTest (4 tests)*
- ✅ POST /items — создание товара
- ✅ Валидация: отсутствие title
- ✅ Валидация: отрицательная цена
- ✅ Валидация: отсутствие цены

**Repository Tests (5 tests)**

*ProductRepositoryTest (3 tests)*
- ✅ Поиск по title/description с пагинацией
- ✅ findAllBy с пагинацией
- ✅ Подсчет количества товаров

*CartItemRepositoryTest (2 tests)*
- ✅ findByProductId
- ✅ findByProductIdIn

**Fallback Chain Tests (3 tests)**

*ServiceChainFallbackTest (3 tests, @WebFluxTest + real CartService + real CheckoutService)*
- ✅ GET /cart/items — Redis бросает ClassCastException → onErrorResume → реальный вызов PaymentClient
- ✅ GET /cart/items — Redis пуст, PaymentClient недоступен → страница рендерится с balance=0
- ✅ POST /buy — Circuit Breaker fallback из processPayment → реальный CheckoutService → редирект

**Cache Tests (5 tests)**

*BalanceCacheIntegrationTest (5 tests, Testcontainers Redis)*
- ✅ Кэш-хит — PaymentClient не вызывается
- ✅ Промах кэша — баланс запрошен и сохранён в Redis
- ✅ Сервис недоступен — fallback не кэшируется
- ✅ evictBalanceCache — ключ удаляется из Redis
- ✅ После успешной оплаты — кэш инвалидирован, следующий запрос обращается к сервису

**Tools:** `@WebFluxTest`, `@DataR2dbcTest`, `@SpringBootTest`, WebTestClient, H2, Testcontainers

## payment-service (19 tests)

### Unit Tests (8)

**PaymentServiceTest (8 tests)**

*getBalance()*
- ✅ Получение существующего баланса
- ✅ Создание начального баланса для нового пользователя
- ✅ Использование default при пустом Redis

*processPayment()*
- ✅ Успешная оплата
- ✅ Недостаточно средств
- ✅ Оплата точной суммы баланса
- ✅ Оплата для нового пользователя
- ✅ Отказ при сумме больше баланса

**Tools:** JUnit 5, Mockito, Reactor Test (StepVerifier), AssertJ

### Integration Tests (10)

**PaymentControllerTest (10 tests)**

*GET /balance*
- ✅ Успешное получение баланса
- ✅ Получение баланса для нового пользователя
- ✅ Валидация отсутствующего параметра

*POST /payment*
- ✅ Успешная обработка платежа
- ✅ Обработка недостаточных средств
- ✅ Валидация: отсутствие userId
- ✅ Валидация: отсутствие amount
- ✅ Валидация: отрицательная сумма
- ✅ Валидация: нулевая сумма
- ✅ Обработка невалидного JSON

**Tools:** `@WebFluxTest`, WebTestClient, MockBean


## Test Configuration

### market-web/src/test/resources/application.properties
```properties
# H2 in-memory database
spring.r2dbc.url=r2dbc:h2:mem:///testdb
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql

# Allow bean overriding
spring.main.allow-bean-definition-overriding=true

# Cache TTL
cache.product-list.ttl-minutes=5

# Payment service URL
payment-service.url=http://localhost:8081
```

### payment-service/src/test/resources/application.properties
```properties
payment.initial-balance=100000

spring.main.allow-bean-definition-overriding=true

spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
```

## Tools & Frameworks

| Инструмент | Назначение |
|-----------|------------|
| **JUnit 5** | Test framework |
| **Mockito** | Mocking framework |
| **AssertJ** | Fluent assertions |
| **Reactor Test** | StepVerifier для реактивных типов |
| **Spring Boot Test** | @SpringBootTest, @WebFluxTest, @DataR2dbcTest |
| **WebTestClient** | Тестирование WebFlux контроллеров |
| **H2** | In-memory database для тестов |

