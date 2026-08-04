package com.travel.insurance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TravelInsuranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelInsuranceApplication.class, args);
    }
}
