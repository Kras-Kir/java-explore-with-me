package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.EventFullDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.model.Event;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsMapper {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;

    public List<EventShortDto> toEventShortDtoList(List<Event> events,
                                                   Map<Long, Long> confirmedRequestsMap,
                                                   Map<Long, Long> viewsMap) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        return events.stream()
                .map(event -> {
                    try {
                        Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                        Long views = viewsMap.getOrDefault(event.getId(), 0L);

                        return EventShortDto.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .annotation(event.getAnnotation())
                                .category(event.getCategory() != null ?
                                        categoryMapper.toCategoryDto(event.getCategory()) : null)
                                .initiator(event.getInitiator() != null ?
                                        userMapper.toUserShortDto(event.getInitiator()) : null)
                                .eventDate(event.getEventDate())
                                .paid(event.getPaid())
                                .confirmedRequests(confirmedRequests)
                                .views(views)
                                .build();
                    } catch (Exception e) {
                        // Логируем ошибку но продолжаем обработку других событий
                        return null;
                    }
                })
                .filter(event -> event != null)
                .collect(Collectors.toList());
    }


    public List<EventFullDto> toEventFullDtoList(List<Event> events,
                                                 Map<Long, Long> confirmedRequestsMap,
                                                 Map<Long, Long> viewsMap) {
        log.info("=== STATS_MAPPER DIAGNOSTICS ===");
        log.info("Input events: {}", events.stream().map(Event::getId).collect(Collectors.toList()));
        log.info("ConfirmedRequestsMap: {}", confirmedRequestsMap);
        log.info("ViewsMap: {}", viewsMap);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);

                    log.info("Mapping event {}: confirmedRequests={}, views={}",
                            event.getId(), confirmedRequests, views);

                    EventFullDto dto = EventFullDto.builder()
                            .id(event.getId())
                            .title(event.getTitle())
                            .annotation(event.getAnnotation())
                            .description(event.getDescription())
                            .category(categoryMapper.toCategoryDto(event.getCategory()))
                            .initiator(userMapper.toUserShortDto(event.getInitiator()))
                            .createdOn(event.getCreatedOn())
                            .eventDate(event.getEventDate())
                            .publishedOn(event.getPublishedOn())
                            .location(locationMapper.toLocationDto(event.getLocation()))
                            .paid(event.getPaid())
                            .participantLimit(event.getParticipantLimit())
                            .requestModeration(event.getRequestModeration())
                            .state(event.getState())
                            .confirmedRequests(confirmedRequests)  // ← Устанавливаем значение
                            .views(views)                         // ← Устанавливаем значение
                            .build();

                    log.info("Built DTO for event {}: confirmedRequests={}",
                            event.getId(), dto.getConfirmedRequests());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
