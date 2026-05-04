# my-market-app

Мультимодульный Maven-проект на Spring Boot с реактивным стеком (WebFlux + R2DBC).

## Модули

| Модуль | Описание | Порт | Статус |
|---|---|---|---|
| **market-web** | Веб-приложение «Витрина интернет-магазина» | 8080 | ✅ Реализовано |
| **payment-service** | RESTful-сервис платежей | 8081 | ✅ Реализовано |

## Функциональность

### market-web

**UI (Thymeleaf):**
- ✅ Витрина товаров с поиском по названию/описанию, сортировкой (по цене, по алфавиту) и пагинацией (2 / 5 / 10 / 20 / 50 / 100 товаров на странице)
- ✅ Страница отдельного товара
- ✅ Корзина покупателя — добавление, изменение количества, удаление товаров
- ✅ Оформление заказа с проверкой баланса в payment-service
- ✅ Очистка корзины после успешного оформления заказа
- ✅ История заказов и страница конкретного заказа
- ✅ Отображение баланса пользователя в корзине

### payment-service

- ✅ Получение баланса*
- ✅ Обработка платежа

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
| Кеширование | Redis (Reactive) + Lettuce |
| REST клиент | WebClient (реактивный) |
| API документация | OpenAPI 3.0 + Swagger UI |
| Кодогенерация | openapi-generator-maven-plugin |
| Маппинг DTO | MapStruct 1.6.3 |



## Маршруты

### market-web UI (Thymeleaf)

| Метод | URL | Описание |
|---|---|---|
| `GET` | `/` или `/items` | Витрина товаров (поиск, сортировка, пагинация) |
| `POST` | `/items` | Изменить количество товара в корзине с витрины |
| `GET` | `/items/{id}` | Страница товара |
| `POST` | `/items/{id}` | Изменить количество товара в корзине со страницы товара |
| `GET` | `/cart/items` | Корзина (с балансом пользователя) |
| `POST` | `/cart/items` | Изменить количество товара в корзине |
| `POST` | `/buy` | **Оформить заказ** (проверка баланса + очистка корзины) |
| `GET` | `/orders` | Список всех заказов |
| `GET` | `/orders/{id}` | Страница заказа |

### payment-service REST API

| Метод | URL | Описание |
|---|---|---|
| `GET` | `/balance?userId={id}` | Получить баланс пользователя |
| `POST` | `/payment` | Обработать платеж |

## Требования к окружению

- **Java 21**
- **Maven** (или используйте обёртку `./mvnw`)
- **Docker Desktop** — для запуска через Docker Compose

Для локального запуска дополнительно:
- **MySQL 8** на `localhost:3306`
- **Redis** на `localhost:6379`

## Настройка переменных окружения

Приложение читает параметры подключения из переменных окружения. Скопируйте пример и заполните значения:

```bash
cp .env.example .env
```

Содержимое `.env`:

```bash
# market-web
R2DBC_URL=r2dbc:mysql://localhost:3306/my_market?connectionTimeZone=UTC
DB_USERNAME=root
DB_PASSWORD=your_password_here
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
PAYMENT_SERVICE_URL=http://localhost:8081

# payment-service
PAYMENT_R2DBC_URL=r2dbc:mysql://localhost:3306/payment_db?connectionTimeZone=UTC
PAYMENT_DB_USERNAME=root
PAYMENT_DB_PASSWORD=your_password_here

# Docker Compose только
MYSQL_ROOT_PASSWORD=your_root_password_here
MYSQL_DATABASE=my_market
MYSQL_USER=user
MYSQL_PASSWORD=your_user_password_here
```

## Сборка и запуск

### Docker Compose (рекомендуется)

Поднимает все сервисы одной командой — настройка БД и Redis не требуется.

```bash
docker-compose up --build
```

| Сервис | Адрес |
|---|---|
| **market-web** | `http://localhost:9090` |
| **payment-service** | `http://localhost:8081` |
| MySQL | `localhost:3307` |
| Redis | `localhost:6379` |

Для остановки:

```bash
docker-compose down
```

Чтобы также удалить данные БД:

```bash
docker-compose down -v
```

### Локально (требуется MySQL и Redis)

```bash
# Сборка всех модулей (включая генерацию OpenAPI кода)
./mvnw clean package

# Сборка конкретного модуля
./mvnw clean package -pl market-web
./mvnw clean package -pl payment-service

# Запуск payment-service (должен быть запущен ПЕРВЫМ)
java -jar payment-service/target/payment-service-0.0.1-SNAPSHOT.jar

# Запуск market-web (в отдельном терминале)
java -jar market-web/target/market-web-0.0.1-SNAPSHOT.jar
```

Приложения будут доступны на:
- market-web: `http://localhost:8080`
- payment-service: `http://localhost:8081`



## OpenAPI Спецификации

- **market-web**: `market-web/src/main/resources/openapi/api.yaml`
- **payment-service**: `payment-service/src/main/resources/openapi/api.yaml`

Серверный код генерируется автоматически при сборке (`mvn package`).
