# my-market-app

Веб-приложение «Витрина интернет-магазина» на Spring Boot с реактивным стеком (WebFlux + R2DBC) и серверным рендерингом через Thymeleaf.

## Функциональность

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
# Сборка
./mvnw clean package

# Запуск (переменные окружения должны быть установлены)
java -jar target/my-market-app-0.0.1-SNAPSHOT.jar
```

Приложение будет доступно на `http://localhost:8080`.

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

Тесты используют H2 in-memory через R2DBC — MySQL не требуется. Схема инициализируется из `src/test/resources/schema.sql`.

```bash
# Все тесты
./mvnw test

# Один класс
./mvnw test -Dtest=ProductServiceTest

# Один метод
./mvnw test -Dtest=ProductServiceTest#methodName
```

| Путь | Тип | Инструменты |
|---|---|---|
| `test/unit/` | Юнит-тесты | JUnit 5, Mockito, AssertJ, Reactor Test (StepVerifier) |
| `test/integration/` | Интеграционные тесты | `@WebFluxTest` + WebTestClient, `@DataR2dbcTest` |