package ru.practicum.server;

import org.mapstruct.Mapper;
import ru.practicum.dto.EndpointHit;


@Mapper(componentModel = "spring")
public interface HitMapper {
    Hit toHit(EndpointHit endpointHit);
}