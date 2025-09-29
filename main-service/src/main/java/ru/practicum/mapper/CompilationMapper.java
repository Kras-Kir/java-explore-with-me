package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface CompilationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilationDto newCompilationDto);

    CompilationDto toCompilationDto(Compilation compilation);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateCompilationFromRequest(UpdateCompilationRequest updateCompilationRequest, @MappingTarget Compilation compilation);

    /*default Set<Event> mapEventIdsToEvents(Set<Long> eventIds) {
        if (eventIds == null) {
            return Set.of();
        }
        return eventIds.stream()
                .map(id -> {
                    Event event = new Event();
                    event.setId(id);
                    return event;
                })
                .collect(Collectors.toSet());
    }

    default Set<Long> mapEventsToEventIds(Set<Event> events) {
        if (events == null) {
            return Set.of();
        }
        return events.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
    }*/
}
