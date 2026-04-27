# my-market-app

Мультимодульный Maven-проект на Spring Boot с реактивным стеком (WebFlux + R2DBC).

## Модули

| Модуль | Описание | Порт |
|---|---|---|
| **market-web** | Веб-приложение «Витрина интернет-магазина» с Thymeleaf | 8080 |
| **payment-service** | RESTful-сервис платежей (структура создана, реализация в разработке) | 8081 |

## Функциональность (market-web)

- Витрина товаров с поиском по названию/описанию, сортировкой (по цене, по алфавиту) и пагинацией (2 / 5 / 10 / 20 / 50 / 100 товаров на странице)
- Страница отдельного товара
- Корзина покупателя — добавление, изменение количества, удаление товаров
- Оформление заказа одним действием (`POST /buy`)
- История заказов и страница конкретного заказа

## Стек

| Слой | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.1 |
| Веб | Spring WebFlux (реактивный) |
| Шаблоны | Thymeleaf |
| Реактивные типы | Project Reactor (Mono, Flux) |
| Доступ к БД | Spring Data R2DBC |
| БД (prod) | MySQL 8 (через R2DBC MySQL driver) |
| БД (тесты) | H2 (in-memory, через R2DBC H2) |
| Маппинг DTO | MapStruct 1.6.3 |

## Архитектура

```
controller/   — Spring WebFlux контроллеры (Thymeleaf + реактивные эндпоинты)
service/      — бизнес-логика, пагинация, агрегация корзины (реактивная)
repository/   — Spring Data R2DBC репозитории
model/        — R2DBC сущности: Product, CartItem, Order, OrderItem
dto/          — DTO, включая Paging для состояния пагинации
mapper/       — MapStruct-маппер
```

Все слои используют реактивные типы `Mono<T>` и `Flux<T>` из Project Reactor.

## Маршруты

| Метод | URL | Описание |
|---|---|---|
| `GET` | `/` или `/items` | Витрина товаров (поиск, сортировка, пагинация) |
| `POST` | `/items` | Изменить количество товара в корзине с витрины |
| `GET` | `/items/{id}` | Страница товара |
| `POST` | `/items/{id}` | Изменить количество товара в корзине со страницы товара |
| `GET` | `/cart/items` | Корзина |
| `POST` | `/cart/items` | Изменить количество товара в корзине |
| `POST` | `/buy` | Оформить заказ из корзины |
| `GET` | `/orders` | Список всех заказов |
| `GET` | `/orders/{id}` | Страница заказа |

## Требования к окружению

- **Java 21**
- **Maven** (или используйте обёртку `./mvnw`)
- **MySQL 8** — для локального запуска без Docker

## Настройка переменных окружения

Приложение читает параметры подключения из переменных окружения. Скопируйте пример и заполните значения:

```bash
cp .env.example .env
```

Содержимое `.env`:

```
R2DBC_URL=r2dbc:mysql://localhost:3306/my_market?connectionTimeZone=UTC
DB_USERNAME=root
DB_PASSWORD=your_password_here

# только для Docker Compose
MYSQL_ROOT_PASSWORD=your_root_password_here
MYSQL_DATABASE=my_market
MYSQL_USER=user
MYSQL_PASSWORD=your_user_password_here
```

**Важно**: Для R2DBC используется префикс `r2dbc:mysql://` вместо `jdbc:mysql://`, а параметр часового пояса — `connectionTimeZone` вместо `serverTimezone`.

## Сборка и запуск

### Локально (требуется MySQL на `localhost:3306`)

```bash
# Сборка всех модулей
./mvnw clean package

# Сборка конкретного модуля
./mvnw clean package -pl market-web
./mvnw clean package -pl payment-service

# Запуск market-web (переменные окружения должны быть установлены)
java -jar market-web/target/market-web-0.0.1-SNAPSHOT.jar

# Запуск payment-service
java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar
```

Приложение market-web будет доступно на `http://localhost:8080`.

### Docker Compose (рекомендуется)

Поднимает приложение и MySQL одной командой — отдельная MySQL не нужна.

```bash
docker-compose up --build
```

| Сервис | Адрес |
|---|---|
| Приложение | `http://localhost:9090` |
| MySQL | `localhost:3307` |

Для остановки:

```bash
docker-compose down
```

Чтобы также удалить данные БД:

```bash
docker-compose down -v
```

## Тесты

Тесты используют H2 in-memory через R2DBC — MySQL не требуется. Схема инициализируется из `market-web/src/test/resources/schema.sql`.

```bash
# Все тесты во всех модулях
./mvnw test

# Тесты конкретного модуля
./mvnw test -pl market-web
./mvnw test -pl payment-service

# Один класс в модуле
./mvnw test -pl market-web -Dtest=ProductServiceTest

# Один метод
./mvnw test -pl market-web -Dtest=ProductServiceTest#methodName
```

| Модуль | Путь | Тип | Инструменты |
|---|---|---|---|
| market-web | `test/unit/` | Юнит-тесты | JUnit 5, Mockito, AssertJ, Reactor Test (StepVerifier) |
| market-web | `test/integration/` | Интеграционные тесты | `@WebFluxTest` + WebTestClient, `@DataR2dbcTest` |
| payment-service | - | Не реализовано | - |