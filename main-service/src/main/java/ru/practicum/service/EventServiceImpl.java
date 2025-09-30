package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.*;
import ru.practicum.model.enums.RequestStatus;
import ru.practicum.repository.*;
import ru.practicum.service.client.StatsClient;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.dto.UpdateEventUserRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.enums.EventState;
import ru.practicum.service.dto.ViewStats;
import ru.practicum.validator.DateValidator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final StatsMapper statsMapper;
    private final StatsClient statsClient;
    private final DateValidator dateValidator;
    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем за 2 часа от текущего момента");
        }

        if (newEventDto.getParticipantLimit() != null && newEventDto.getParticipantLimit() < 0) {
            throw new ValidationException("Лимит участников не может быть отрицательным");
        }

        if (newEventDto.getAnnotation().length() > 2000) {
            throw new ValidationException("Annotation must be between 20 and 2000 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена"));

        // 1. СОХРАНЯЕМ LOCATION В БАЗУ
        Location location = locationMapper.toLocation(newEventDto.getLocation());
        Location savedLocation = locationRepository.save(location);
        log.info("Создана локация с id: {}", savedLocation.getId());

        // 2. СОЗДАЕМ EVENT
        Event event = eventMapper.toEvent(newEventDto);
        event.setInitiator(user);
        event.setCategory(category);
        event.setLocation(savedLocation); // ← УСТАНАВЛИВАЕМ СОХРАНЕННУЮ LOCATION

        Event savedEvent = eventRepository.save(event);
        log.info("Создано событие с id: {}", savedEvent.getId());

        return eventMapper.toEventFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiator(user, pageable).getContent();

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);

        return statsMapper.toEventShortDtoList(events, confirmedRequests, views);
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findByIdAndInitiator(eventId, user)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " у пользователя с id=" + userId + " не найдено"));

        Long confirmedRequests = requestRepository.countByEventAndStatus(event, ru.practicum.model.enums.RequestStatus.CONFIRMED);
        Long views = getViews(List.of(event)).getOrDefault(eventId, 0L);

        return eventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateEvent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findByIdAndInitiator(eventId, user)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " у пользователя с id=" + userId + " не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }

        if (updateEvent.getEventDate() != null && updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Дата события должна быть не ранее чем за 2 часа от текущего момента");
        }

        eventMapper.updateEventFromUserRequest(updateEvent, event);

        if (updateEvent.getCategory() != null) {
            Category category = categoryRepository.findById(updateEvent.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + updateEvent.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        Event updatedEvent = eventRepository.save(event);

        Long confirmedRequests = requestRepository.countByEventAndStatus(event, ru.practicum.model.enums.RequestStatus.CONFIRMED);
        Long views = getViews(List.of(event)).getOrDefault(eventId, 0L);

        return eventMapper.toEventFullDto(updatedEvent, confirmedRequests, views);
    }


    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> searchEventsByAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Integer from, Integer size) {
        try {
            log.info("=== SEARCH_EVENTS_BY_ADMIN OPTIMIZED ===");

            // 1. Сначала найти ID событий которые подходят под фильтры
            List<Event> filteredEvents = eventRepository.findAll().stream()
                    .filter(event -> users == null || users.isEmpty() || users.contains(event.getInitiator().getId()))
                    .filter(event -> states == null || states.isEmpty() || states.contains(event.getState()))
                    .filter(event -> categories == null || categories.isEmpty() || categories.contains(event.getCategory().getId()))
                    .filter(event -> rangeStart == null || event.getEventDate().isAfter(rangeStart))
                    .filter(event -> rangeEnd == null || event.getEventDate().isBefore(rangeEnd))
                    .collect(Collectors.toList());

            // 2. Пагинация
            int start = Math.min(from, filteredEvents.size());
            int end = Math.min(from + size, filteredEvents.size());
            List<Event> paginatedEvents = filteredEvents.subList(start, end);

            // 3. Получить confirmedRequests только для пагинированных событий
            Map<Long, Long> confirmedRequests = getConfirmedRequests(paginatedEvents);
            Map<Long, Long> views = getViews(paginatedEvents);

            return statsMapper.toEventFullDtoList(paginatedEvents, confirmedRequests, views);

        } catch (Exception e) {
            log.error("Error in searchEventsByAdmin: ", e);
            throw new RuntimeException("Failed to search events", e);
        }
    }


    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateEvent) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (updateEvent.getEventDate() != null && updateEvent.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ValidationException("Дата события должна быть не ранее чем за 1 час от текущего момента");
        }

        if (updateEvent.getStateAction() != null) {
            if (updateEvent.getStateAction() == ru.practicum.model.enums.EventStateAction.PUBLISH_EVENT &&
                    event.getState() != EventState.PENDING) {
                throw new ConflictException("Нельзя опубликовать событие, которое не в состоянии ожидания публикации");
            }

            if (updateEvent.getStateAction() == ru.practicum.model.enums.EventStateAction.REJECT_EVENT &&
                    event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя отклонить уже опубликованное событие");
            }
        }

        eventMapper.updateEventFromAdminRequest(updateEvent, event);

        if (updateEvent.getCategory() != null) {
            Category category = categoryRepository.findById(updateEvent.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + updateEvent.getCategory() + " не найдена"));
            event.setCategory(category);
        }

        Event updatedEvent = eventRepository.save(event);

        Long confirmedRequests = requestRepository.countByEventAndStatus(event, ru.practicum.model.enums.RequestStatus.CONFIRMED);
        Long views = getViews(List.of(event)).getOrDefault(eventId, 0L);

        return eventMapper.toEventFullDto(updatedEvent, confirmedRequests, views);
    }


    @Override
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }

        try {
            log.info("Getting public events with params: text={}, categories={}, paid={}", text, categories, paid);

            dateValidator.validatePaginationParams(from, size);

            // Устанавливаем диапазон дат по умолчанию
            if (rangeStart == null) {
                rangeStart = LocalDateTime.now();
            }

            // Если rangeEnd не указан, устанавливаем его на далекое будущее
            if (rangeEnd == null) {
                rangeEnd = LocalDateTime.now().plusYears(100);
            }

            // Получаем базовый список опубликованных событий БЕЗ пагинации
            List<Event> allPublishedEvents = eventRepository.findByState(EventState.PUBLISHED);

            // Фильтруем по всем параметрам
            List<Event> filteredEvents = filterEvents(allPublishedEvents, text, categories, paid, rangeStart, rangeEnd, onlyAvailable);

            // Сортируем
            filteredEvents = sortEvents(filteredEvents, sort);

            // Применяем пагинацию вручную
            int startIndex = Math.min(from, filteredEvents.size());
            int endIndex = Math.min(from + size, filteredEvents.size());
            List<Event> paginatedEvents = filteredEvents.subList(startIndex, endIndex);

            Map<Long, Long> confirmedRequests = getConfirmedRequests(paginatedEvents);
            Map<Long, Long> views = getViews(paginatedEvents);

            List<EventShortDto> result = statsMapper.toEventShortDtoList(paginatedEvents, confirmedRequests, views);

            // Дополнительная сортировка по просмотрам для DTO
            if ("VIEWS".equals(sort)) {
                result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
            }

            log.info("Returning {} events", result.size());
            return result;

        } catch (Exception e) {
            log.error("Error in getPublicEvents: ", e);
            throw new RuntimeException("Failed to get public events", e);
        }
    }

    @Override
    public void saveHit(String app, String uri, String ip) {
        try {
            statsClient.saveHit(app, uri, ip);
            log.debug("Hit сохранен: app={}, uri={}, ip={}", app, uri, ip);
        } catch (Exception e) {
            log.warn("Ошибка сохранения hit: {}", e.getMessage());
        }
    }


    private List<Event> getBasePublishedEvents() {
        return eventRepository.findByState(EventState.PUBLISHED);
    }


    private List<Event> filterEvents(List<Event> events, String text, List<Long> categories,
                                     Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                     Boolean onlyAvailable) {
        return events.stream()
                .filter(event -> text == null || text.isBlank() ||
                        containsText(event, text))
                .filter(event -> categories == null || categories.isEmpty() ||
                        categories.contains(event.getCategory().getId()))
                .filter(event -> paid == null || event.getPaid().equals(paid))
                .filter(event -> event.getEventDate().isAfter(rangeStart))
                .filter(event -> event.getEventDate().isBefore(rangeEnd))
                .filter(event -> !Boolean.TRUE.equals(onlyAvailable) || isEventAvailable(event))
                .collect(Collectors.toList());
    }

    private boolean containsText(Event event, String text) {
        String searchText = text.toLowerCase();
        return event.getAnnotation().toLowerCase().contains(searchText) ||
                event.getDescription().toLowerCase().contains(searchText);
    }


    private List<Event> sortEvents(List<Event> events, String sort) {
        List<Event> sortedEvents = new ArrayList<>(events);

        if ("EVENT_DATE".equals(sort)) {
            sortedEvents.sort(Comparator.comparing(Event::getEventDate));
        }

        return sortedEvents;
    }



    private boolean isEventAvailable(Event event) {
        if (event.getParticipantLimit() == 0) {
            return true;
        }
        try {
            Long confirmedRequests = requestRepository.countByEventAndStatus(
                    event, RequestStatus.CONFIRMED);
            return confirmedRequests < event.getParticipantLimit();
        } catch (Exception e) {
            log.warn("Error checking event availability for event {}: {}", event.getId(), e.getMessage());
            return true;
        }
    }


    @Override
    public EventFullDto getPublicEventById(Long eventId, String clientIp) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано");
        }

        // 1. СОХРАНЯЕМ HIT
        saveHit("ewm-main-service", "/events/" + eventId, clientIp);

        // 2. ПОЛУЧАЕМ СТАТИСТИКУ ТОЛЬКО ДЛЯ УНИКАЛЬНЫХ IP
        Long views = getViews(List.of(event)).getOrDefault(eventId, 0L);

        log.info("Событие {}: уникальные просмотры = {} (IP: {})", eventId, views, clientIp);

        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);

        return eventMapper.toEventFullDto(event, confirmedRequests, views);
    }


    private Map<Long, Long> getConfirmedRequests(List<Event> events) {

        if (events.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> result = new HashMap<>();

        for (Event event : events) {
            try {
                List<ParticipationRequest> allRequests = requestRepository.findByEvent(event);

                List<ParticipationRequest> confirmedRequests = requestRepository.findByEventAndStatus(event, RequestStatus.CONFIRMED);

                Long count = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);

                result.put(event.getId(), count != null ? count : 0L);
            } catch (Exception e) {
                result.put(event.getId(), 0L);
            }
        }

        return result;
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        try {
            LocalDateTime start = LocalDateTime.now().minusYears(1);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = events.stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());

            // Получаем статистику
            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            Map<Long, Long> result = parseViewStats(stats);

            return result;

        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
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
                        stats -> extractEventIdFromUri(stats.getUri()), // Извлекаем eventId из URI
                        ViewStats::getHits,
                        (existing, replacement) -> existing // При дубликатах берем существующее значение
                ));
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            // URI имеет формат "/events/123" - извлекаем "123"
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.warn("Не удалось извлечь eventId из URI: {}", uri);
            return null;
        }
    }

}
