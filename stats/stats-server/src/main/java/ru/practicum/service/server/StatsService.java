package ru.practicum.service.server;


import ru.practicum.service.dto.EndpointHit;
import ru.practicum.service.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void saveHit(EndpointHit endpointHit);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique);
}