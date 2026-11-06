package com.travel.insurance.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HttpClientConfigTest {

    @Test
    void appliesConnectAndReadTimeoutsToTheRestClientBuilder() {
        RestClientCustomizer customizer = new HttpClientConfig()
                .outboundTimeoutsCustomizer(Duration.ofSeconds(5), Duration.ofSeconds(15));
        RestClient.Builder builder = RestClient.builder();

        customizer.customize(builder);

        Object factory = ReflectionTestUtils.getField(builder, "requestFactory");
        assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(ReflectionTestUtils.getField(factory, "connectTimeout")).isEqualTo(5_000);
        assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(15_000);
    }
}
