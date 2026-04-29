package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${payment-service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public WebClient paymentWebClient() {
        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }
}
