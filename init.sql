CREATE DATABASE IF NOT EXISTS my_market CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE my_market;
SET NAMES utf8mb4;

DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(2000),
    img_path   VARCHAR(255),
    price      BIGINT,
    title      VARCHAR(255)
);

CREATE TABLE orders (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_sum BIGINT
);

CREATE TABLE cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    count      INT NOT NULL,
    product_id BIGINT
);

CREATE TABLE order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    count      INT NOT NULL,
    price      BIGINT,
    order_id   BIGINT,
    product_id BIGINT
);

-- Sample data
INSERT INTO products (title, description, img_path, price) VALUES
('Футбольный мяч', 'Профессиональный мяч для футбола', '/images/football.jpg', 2500),
('Баскетбольный мяч', 'Мяч для баскетбола размер 7', '/images/basketball.jpg', 3000),
('Волейбольный мяч', 'Мяч для волейбола', '/images/volleyball.jpg', 2000),
('Теннисный мяч', 'Набор теннисных мячей', '/images/tennis.jpg', 500),
('Мяч для регби', 'Овальный мяч для регби', '/images/rugby.jpg', 3500);