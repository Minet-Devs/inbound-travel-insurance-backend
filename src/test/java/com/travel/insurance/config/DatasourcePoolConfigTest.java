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
}
