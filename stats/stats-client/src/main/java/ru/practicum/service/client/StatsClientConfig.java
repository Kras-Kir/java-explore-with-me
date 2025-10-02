package ru.practicum.service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatsClientConfig {

    @Value("${STATS_SERVER_URL:http://localhost:9090}")
    private String serverUrl;

    @Bean
    public StatsClient statsClient() {
        return new StatsClient(serverUrl);
    }
}
