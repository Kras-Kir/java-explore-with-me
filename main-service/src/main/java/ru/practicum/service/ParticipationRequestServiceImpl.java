package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.model.enums.RequestStatus;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestMapper requestMapper;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        // ДЕТАЛЬНАЯ ДИАГНОСТИКА
        log.info("=== ДИАГНОСТИКА ЗАЯВКИ ДЛЯ СОБЫТИЯ {} ===", eventId);
        log.info("Event.requestModeration: {} (type: {})",
                event.getRequestModeration(),
                event.getRequestModeration() != null ? event.getRequestModeration().getClass().getSimpleName() : "null");
        log.info("Event.participantLimit: {} (type: {})",
                event.getParticipantLimit(),
                event.getParticipantLimit() != null ? event.getParticipantLimit().getClass().getSimpleName() : "null");

        boolean needsModeration = Boolean.TRUE.equals(event.getRequestModeration())
                && event.getParticipantLimit() != null
                && event.getParticipantLimit() > 0;

        log.info("Условие PENDING: {} && {} && {} = {}",
                Boolean.TRUE.equals(event.getRequestModeration()),
                event.getParticipantLimit() != null,
                event.getParticipantLimit() != null && event.getParticipantLimit() > 0,
                needsModeration);

        RequestStatus status = needsModeration ? RequestStatus.PENDING : RequestStatus.CONFIRMED;
        log.info("УСТАНОВЛЕН СТАТУС: {}", status);
        log.info("===============================");

        // Проверка, что пользователь не инициатор события
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие в своём событии");
        }

        // Проверка, что событие опубликовано
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // Проверка, что заявка уже существует (используем existsBy)
        if (requestRepository.existsByEventAndRequester(event, user)) {
            throw new ConflictException("Заявка на участие в этом событии уже существует");
        }

        // Проверка лимита участников
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников события");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(user)
                .created(LocalDateTime.now())
                .status(event.getRequestModeration() && event.getParticipantLimit() > 0 ?
                        RequestStatus.PENDING : RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("Создана заявка на участие с id: {}", savedRequest.getId());

        return requestMapper.toParticipationRequestDto(savedRequest);
    }


    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        return requestRepository.findByRequester(user).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Заявка с id=" + requestId + " не принадлежит пользователю с id=" + userId);
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);

        return requestMapper.toParticipationRequestDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не является инициатором события с id=" + eventId);
        }

        return requestRepository.findByEvent(event).stream()
                .map(requestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не является инициатором события с id=" + eventId);
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Подтверждение заявок не требуется для этого события");
        }

        // Проверка лимита участников
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников события");
        }

        List<ParticipationRequest> requests = requestRepository.findByIdIn(updateRequest.getRequestIds());

        // Проверка, что найдены все запрошенные заявки
        if (requests.size() != updateRequest.getRequestIds().size()) {
            throw new NotFoundException("Некоторые заявки не найдены");
        }

        // Проверка, что все заявки принадлежат указанному событию
        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                throw new ValidationException("Заявка с id=" + request.getId() + " не принадлежит событию с id=" + eventId);
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии ожидания");
            }

            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (confirmedRequests < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests++;
                    confirmed.add(requestMapper.toParticipationRequestDto(request));
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(requestMapper.toParticipationRequestDto(request));
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(requestMapper.toParticipationRequestDto(request));
            }
        }

        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed)
                .rejectedRequests(rejected)
                .build();
    }
}

