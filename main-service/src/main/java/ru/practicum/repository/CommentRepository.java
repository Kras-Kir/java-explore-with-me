package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Comment;
import ru.practicum.model.enums.CommentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Поиск одобренных комментариев для события с пагинацией
    List<Comment> findByEventIdAndStatusOrderByCreatedOnDesc(Long eventId, CommentStatus status, Pageable pageable);

    // Поиск комментариев пользователя (всех или к конкретному событию)
    List<Comment> findByAuthorId(Long authorId, Pageable pageable);

    List<Comment> findByAuthorIdAndEventId(Long authorId, Long eventId, Pageable pageable);

    // Поиск комментариев по статусу
    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    // Поиск комментария по ID с проверкой автора
    Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);

    // Проверка существования комментария у пользователя к событию
    boolean existsByAuthorIdAndEventId(Long authorId, Long eventId);

    // Поиск для администратора с фильтрами
    @Query("SELECT c FROM Comment c WHERE " +
            "(:users IS NULL OR c.author.id IN :users) AND " +
            "(:events IS NULL OR c.event.id IN :events) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(c.createdOn BETWEEN :rangeStart AND :rangeEnd)")
    List<Comment> findCommentsByAdmin(@Param("users") List<Long> users,
                                      @Param("events") List<Long> events,
                                      @Param("status") CommentStatus status,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    // Подсчет количества одобренных комментариев для события
    Long countByEventIdAndStatus(Long eventId, CommentStatus status);


}
