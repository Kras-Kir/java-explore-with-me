package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class RequestPrivateController {

    private final ParticipationRequestService requestService;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("Получен запрос на получение заявок пользователя с id {}", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam Long eventId) {
        log.info("Получен запрос на создание заявки от пользователя с id {} на событие с id {}", userId, eventId);
        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("Получен запрос на отмену заявки с id {} пользователя с id {}", requestId, userId);
        return requestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        log.info("Получен запрос на получение заявок события с id {} пользователя с id {}", eventId, userId);
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("Получен запрос на обновление статусов заявок события с id {} пользователя с id {}: {}",
                eventId, userId, updateRequest);
        return requestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}
