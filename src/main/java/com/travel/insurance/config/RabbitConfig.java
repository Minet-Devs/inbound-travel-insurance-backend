package com.travel.insurance.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String DOMAIN_EVENTS_EXCHANGE = "travel.domain-events";

    public static final String CLAIM_APPROVED_KEY = "claim.approved";
    public static final String PREAUTHORIZATION_DECIDED_KEY = "preauthorization.decided";
    public static final String POLICY_ACTIVATED_KEY = "policy.activated";
    public static final String BIOMETRIC_VERIFICATION_RESOLVED_KEY = "biometric-verification.resolved";

    public static final String CLAIM_APPROVED_QUEUE = "travel.claim.approved";
    public static final String PREAUTHORIZATION_DECIDED_QUEUE = "travel.preauthorization.decided";
    public static final String POLICY_ACTIVATED_QUEUE = "travel.policy.activated";
    public static final String BIOMETRIC_VERIFICATION_RESOLVED_QUEUE = "travel.biometric-verification.resolved";

    @Bean
    public TopicExchange domainEventsExchange() {
        return new TopicExchange(DOMAIN_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue claimApprovedQueue() {
        return new Queue(CLAIM_APPROVED_QUEUE, true);
    }

    @Bean
    public Queue preauthorizationDecidedQueue() {
        return new Queue(PREAUTHORIZATION_DECIDED_QUEUE, true);
    }

    @Bean
    public Queue policyActivatedQueue() {
        return new Queue(POLICY_ACTIVATED_QUEUE, true);
    }

    @Bean
    public Binding claimApprovedBinding() {
        return BindingBuilder.bind(claimApprovedQueue()).to(domainEventsExchange()).with(CLAIM_APPROVED_KEY);
    }

    @Bean
    public Binding preauthorizationDecidedBinding() {
        return BindingBuilder.bind(preauthorizationDecidedQueue()).to(domainEventsExchange())
                .with(PREAUTHORIZATION_DECIDED_KEY);
    }

    @Bean
    public Binding policyActivatedBinding() {
        return BindingBuilder.bind(policyActivatedQueue()).to(domainEventsExchange()).with(POLICY_ACTIVATED_KEY);
    }

    @Bean
    public Queue biometricVerificationResolvedQueue() {
        return new Queue(BIOMETRIC_VERIFICATION_RESOLVED_QUEUE, true);
    }

    @Bean
    public Binding biometricVerificationResolvedBinding() {
        return BindingBuilder.bind(biometricVerificationResolvedQueue()).to(domainEventsExchange())
                .with(BIOMETRIC_VERIFICATION_RESOLVED_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
