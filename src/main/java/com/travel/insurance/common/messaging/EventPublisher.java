package com.travel.insurance.common.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.travel.insurance.config.RabbitConfig.DOMAIN_EVENTS_EXCHANGE;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(DOMAIN_EVENTS_EXCHANGE, routingKey, payload);
        } catch (Exception ex) {
            log.error("Failed to publish event [{}]", routingKey, ex);
        }
    }
}
