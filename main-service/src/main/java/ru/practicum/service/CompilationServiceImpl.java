package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.service.client.StatsClient;
import ru.practicum.service.dto.ViewStats;
import ru.practicum.validator.DateValidator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final CompilationMapper compilationMapper;
    private final StatsMapper statsMapper;
    private final DateValidator dateValidator;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        setEventsToCompilation(compilation, newCompilationDto.getEvents());

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Создана подборка с id: {}", savedCompilation.getId());

        return getCompilationWithStats(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compilationId + " не найдена"));

        compilationMapper.updateCompilationFromRequest(updateRequest, compilation);

        if (updateRequest.getEvents() != null) {
            setEventsToCompilation(compilation, updateRequest.getEvents());
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);

        return getCompilationWithStats(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Подборка с id=" + compilationId + " не найдена");
        }

        compilationRepository.deleteById(compilationId);
        log.info("Удалена подборка с id: {}", compilationId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        dateValidator.validatePaginationParams(from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable).getContent();
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return compilations.stream()
                .map(this::getCompilationWithStats)
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compilationId) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compilationId + " не найдена"));

        return getCompilationWithStats(compilation);
    }

    private CompilationDto getCompilationWithStats(Compilation compilation) {
        List<Event> events = new ArrayList<>(compilation.getEvents());

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);

        CompilationDto compilationDto = compilationMapper.toCompilationDto(compilation);

        // Заполняем события с статистикой
        if (!events.isEmpty()) {
            compilationDto.setEvents(statsMapper.toEventShortDtoList(events, confirmedRequests, views));
        }

        return compilationDto;
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        return events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> requestRepository.countByEventAndStatus(event, ru.practicum.model.enums.RequestStatus.CONFIRMED)
                ));
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        try {
            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusYears(1);

            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            return parseViewStats(stats);

        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров для подборки: {}", e.getMessage());
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
    }

    private Map<Long, Long> parseViewStats(List<ViewStats> viewStats) {
        if (viewStats == null || viewStats.isEmpty()) {
            return Map.of();
        }

        return viewStats.stream()
                .filter(stats -> stats.getUri() != null && stats.getHits() != null)
                .collect(Collectors.toMap(
                        stats -> extractEventIdFromUri(stats.getUri()),
                        ViewStats::getHits,
                        (existing, replacement) -> existing
                ));
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Не удалось извлечь eventId из URI: {}", uri);
            return null;
        }
    }

    private void setEventsToCompilation(Compilation compilation, List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            compilation.setEvents(Collections.emptySet());
            return;
        }

        List<Event> events = eventRepository.findByIdIn(eventIds);

        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Некоторые события не найдены");
        }

        compilation.setEvents(new HashSet<>(events));
    }
}
