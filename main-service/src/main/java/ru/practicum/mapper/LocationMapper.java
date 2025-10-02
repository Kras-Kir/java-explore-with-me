package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.dto.LocationDto;
import ru.practicum.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {


    @Mapping(target = "id", ignore = true)
    Location toLocation(LocationDto locationDto);

    LocationDto toLocationDto(Location location);
}
