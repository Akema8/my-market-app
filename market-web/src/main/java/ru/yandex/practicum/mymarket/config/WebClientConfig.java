package ru.yandex.practicum.mymarket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${payment-service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public WebClient paymentWebClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        var oauth2Filter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        oauth2Filter.setDefaultClientRegistrationId("market-web-client");
        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .filter(oauth2Filter)
                .build();
    }
}
