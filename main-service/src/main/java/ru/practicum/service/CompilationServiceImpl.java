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
import ru.practicum.service.CompilationService;
import ru.practicum.validator.DateValidator;

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

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findByIdIn(newCompilationDto.getEvents());
            // Проверяем, что все события найдены
            if (events.size() != newCompilationDto.getEvents().size()) {
                throw new NotFoundException("Некоторые события не найдены");
            }
            compilation.setEvents(new HashSet<>(events));
        }

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
            List<Event> events = eventRepository.findByIdIn(updateRequest.getEvents());
            // Проверяем, что все события найдены
            if (events.size() != updateRequest.getEvents().size()) {
                throw new NotFoundException("Некоторые события не найдены");
            }
            compilation.setEvents(new HashSet<>(events));
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

        // Здесь будет логика получения просмотров из сервиса статистики
        // Пока возвращаем заглушку
        return events.stream()
                .collect(Collectors.toMap(Event::getId, event -> 0L));
    }
}
