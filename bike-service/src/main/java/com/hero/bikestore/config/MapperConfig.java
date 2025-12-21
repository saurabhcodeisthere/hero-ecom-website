package com.hero.bikestore.config;

import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper m = new ModelMapper();
        m.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE.PRIVATE);
        return m;
    }
}