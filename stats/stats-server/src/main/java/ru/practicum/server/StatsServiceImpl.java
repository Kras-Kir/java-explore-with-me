package ru.practicum.server;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final HitRepository hitRepository;
    private final HitMapper hitMapper;

    @Override
    public void saveHit(EndpointHit endpointHit) {
        Hit hit = hitMapper.toHit(endpointHit);
        hitRepository.save(hit);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        if (uris == null || uris.isEmpty()) {
            if (unique) {
                return hitRepository.getStatsUniqueIp(start, end);
            } else {
                return hitRepository.getStats(start, end);
            }
        } else {
            if (unique) {
                return hitRepository.getStatsByUrisUniqueIp(start, end, uris);
            } else {
                return hitRepository.getStatsByUris(start, end, uris);
            }
        }
    }
}