package com.allearning.backend.learning.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.random.RandomGenerator;

@Configuration
public class RandomGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean
    public RandomGenerator randomGenerator() {
        return RandomGenerator.getDefault();
    }
}

