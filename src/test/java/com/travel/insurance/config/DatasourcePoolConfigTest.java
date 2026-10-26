package com.travel.insurance.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourcePoolConfigTest {

    @Test
    void hikariLeakDetectionIsEnabledByDefault() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties.getProperty("spring.datasource.hikari.leak-detection-threshold"))
                .isEqualTo("${DB_LEAK_DETECTION_THRESHOLD_MS:20000}");
    }

    @Test
    void keepaliveIsShorterThanPgpoolClientIdleLimit() {
        Properties properties = load();

        assertThat(properties.getProperty("spring.datasource.hikari.keepalive-time"))
                .isEqualTo("${DB_KEEPALIVE_TIME_MS:60000}");
    }

    @Test
    void jdbcSocketReadsAreBounded() {
        Properties properties = load();

        assertThat(properties.getProperty("spring.datasource.hikari.data-source-properties.socketTimeout"))
                .isEqualTo("${DB_SOCKET_TIMEOUT_SECONDS:60}");
        assertThat(properties.getProperty("spring.datasource.hikari.data-source-properties.connectTimeout"))
                .isEqualTo("10");
        assertThat(properties.getProperty("spring.datasource.hikari.data-source-properties.tcpKeepAlive"))
                .isEqualTo("true");
    }

    private static Properties load() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        return yaml.getObject();
    }
}
