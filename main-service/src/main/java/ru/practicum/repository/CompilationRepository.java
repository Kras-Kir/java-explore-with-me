package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> findByPinned(Boolean pinned, Pageable pageable);



    // Проверка существования события в подборке
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Compilation c JOIN c.events e WHERE c.id = :compilationId AND e.id = :eventId")
    Boolean existsEventInCompilation(@Param("compilationId") Long compilationId, @Param("eventId") Long eventId);
}