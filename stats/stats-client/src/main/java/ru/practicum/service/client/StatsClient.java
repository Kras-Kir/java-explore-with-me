package ru.practicum.service.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.practicum.service.dto.EndpointHit;
import ru.practicum.service.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsClient {
    private final String serverUrl;
    private final RestTemplate restTemplate;

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = new RestTemplate();
    }

    public void saveHit(String app, String uri, String ip) {
        try {
            EndpointHit endpointHit = EndpointHit.builder()
                    .app(app)
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EndpointHit> requestEntity = new HttpEntity<>(endpointHit, headers);

            restTemplate.exchange(serverUrl + "/hit", HttpMethod.POST, requestEntity, Object.class);

        } catch (Exception e) {
            System.err.println("Error saving hit: " + e.getMessage());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Используем HashMap вместо Map.of
            Map<String, String> params = new HashMap<>();
            params.put("start", start.format(formatter));
            params.put("end", end.format(formatter));

            StringBuilder url = new StringBuilder(serverUrl + "/stats?start={start}&end={end}");

            if (uris != null && !uris.isEmpty()) {
                url.append("&uris={uris}");
                params.put("uris", String.join(",", uris));
            }
            if (unique != null) {
                url.append("&unique={unique}");
                params.put("unique", unique.toString());
            }

            ResponseEntity<List<ViewStats>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStats>>() {},
                    params
            );

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Error getting stats: " + e.getMessage());
            return List.of(); // возвращаем пустой список вместо null
        }
    }
}