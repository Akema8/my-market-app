/*DROP TABLE IF EXISTS `cart_items`;
CREATE TABLE `cart_items` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `count` int NOT NULL,
                              `product_id` bigint DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FK1re40cjegsfvw58xrkdp6bac6` (`product_id`),
                              CONSTRAINT `FK1re40cjegsfvw58xrkdp6bac6` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
);

DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `count` int NOT NULL,
                               `price` bigint DEFAULT NULL,
                               `order_id` bigint DEFAULT NULL,
                               `product_id` bigint DEFAULT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
                               KEY `FKocimc7dtr037rh4ls4l95nlfi` (`product_id`),
                               CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
                               CONSTRAINT `FKocimc7dtr037rh4ls4l95nlfi` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
);

DROP TABLE IF EXISTS `products`;
CREATE TABLE `products` (
                            `id` bigint NOT NULL AUTO_INCREMENT,
                            `description` varchar(2000) DEFAULT NULL,
                            `img_path` varchar(255) DEFAULT NULL,
                            `price` bigint DEFAULT NULL,
                            `title` varchar(255) DEFAULT NULL,
                            PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `total_sum` bigint DEFAULT NULL,
                          PRIMARY KEY (`id`)
);*/