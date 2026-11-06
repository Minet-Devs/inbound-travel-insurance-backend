package com.travel.insurance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/**
 * Applies connect/read timeouts to every {@code RestClient} built from the auto-configured
 * builder (biometrics microservice, exchange-rate API). Without them an unresponsive upstream
 * blocks the calling request thread indefinitely, and any DB connection it holds with it.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClientCustomizer outboundTimeoutsCustomizer(
            @Value("${app.http-client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.http-client.read-timeout:15s}") Duration readTimeout) {
        return builder -> {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeout);
            requestFactory.setReadTimeout(readTimeout);
            builder.requestFactory(requestFactory);
        };
    }
}
