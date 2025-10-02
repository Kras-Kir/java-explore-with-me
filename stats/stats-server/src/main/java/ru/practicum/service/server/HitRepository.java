package ru.practicum.service.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.service.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface HitRepository extends JpaRepository<Hit, Long> {

    @Query("SELECT new ru.practicum.service.dto.ViewStats(h.app, h.uri, COUNT(h.id)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN ?1 AND ?2 " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.id) DESC")
    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new ru.practicum.service.dto.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN ?1 AND ?2 " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> getStatsUniqueIp(LocalDateTime start, LocalDateTime end);

    @Query("SELECT new ru.practicum.service.dto.ViewStats(h.app, h.uri, COUNT(h.id)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN ?1 AND ?2 " +
            "AND h.uri IN (?3) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h.id) DESC")
    List<ViewStats> getStatsByUris(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("SELECT new ru.practicum.service.dto.ViewStats(h.app, h.uri, COUNT(DISTINCT h.ip)) " +
            "FROM Hit h " +
            "WHERE h.timestamp BETWEEN ?1 AND ?2 " +
            "AND h.uri IN (?3) " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStats> getStatsByUrisUniqueIp(LocalDateTime start, LocalDateTime end, List<String> uris);
}
