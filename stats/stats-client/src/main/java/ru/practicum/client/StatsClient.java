package ru.practicum.client;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    }

    public Object getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder url = new StringBuilder(serverUrl + "/stats?start={start}&end={end}");
        Map<String, String> params = Map.of(
                "start", start.format(formatter),
                "end", end.format(formatter)
        );

        if (uris != null && !uris.isEmpty()) {
            url.append("&uris={uris}");
            params.put("uris", String.join(",", uris));
        }
        if (unique != null) {
            url.append("&unique={unique}");
            params.put("unique", unique.toString());
        }

        ResponseEntity<Object> response = restTemplate.getForEntity(
                url.toString(),
                Object.class,
                params
        );

        return response.getBody();
    }
}