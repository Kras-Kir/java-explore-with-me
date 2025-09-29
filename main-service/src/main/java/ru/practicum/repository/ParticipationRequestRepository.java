package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;
import ru.practicum.model.enums.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequester(User requester);

    List<ParticipationRequest> findByEvent(Event event);

    Optional<ParticipationRequest> findByEventAndRequester(Event event, User requester);

    Long countByEventAndStatus(Event event, RequestStatus status);

    @Query("SELECT pr FROM ParticipationRequest pr WHERE pr.event IN :events AND pr.status = 'CONFIRMED'")
    List<ParticipationRequest> findByEventsAndConfirmed(List<Event> events);

    List<ParticipationRequest> findByIdIn(List<Long> requestIds);

    // Дополнительные методы для проверки существования заявок
    Boolean existsByEventAndRequester(Event event, User requester);

    // Метод для получения подтвержденных заявок по списку событий
    @Query("SELECT pr.event.id, COUNT(pr) FROM ParticipationRequest pr " +
            "WHERE pr.event IN :events AND pr.status = 'CONFIRMED' " +
            "GROUP BY pr.event.id")
    List<Object[]> countConfirmedRequestsByEvents(List<Event> events);
}