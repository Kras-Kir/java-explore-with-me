package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;
import ru.practicum.model.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequester(User requester);

    List<ParticipationRequest> findByEvent(Event event);

    Long countByEventAndStatus(Event event, RequestStatus status);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    Boolean existsByEventAndRequester(Event event, User requester);

    List<ParticipationRequest> findByEventAndStatus(Event event, RequestStatus status);
}