package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Простые методы
    Page<Event> findByInitiator(User initiator, Pageable pageable);

    Optional<Event> findByIdAndInitiator(Long eventId, User initiator);

    List<Event> findByIdIn(List<Long> eventIds);

    Boolean existsByCategoryId(Long categoryId);

    Page<Event> findByState(EventState state, Pageable pageable);

    Page<Event> findByStateAndEventDateAfter(EventState state, LocalDateTime eventDate, Pageable pageable);

    Page<Event> findByStateAndCategoryIdIn(EventState state, List<Long> categoryIds, Pageable pageable);

    Page<Event> findByStateAndPaid(EventState state, Boolean paid, Pageable pageable);

    Boolean existsByInitiatorAndId(User initiator, Long eventId);

    // Убрали все сложные @Query методы!
}