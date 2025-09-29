package ru.practicum.service.server;

import org.mapstruct.Mapper;
import ru.practicum.service.dto.EndpointHit;


@Mapper(componentModel = "spring")
public interface HitMapper {
    Hit toHit(EndpointHit endpointHit);
}