package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    // Поиск локации по координатам (чтобы избежать дублирования)
    @Query("SELECT l FROM Location l WHERE l.lat = :lat AND l.lon = :lon")
    Optional<Location> findByLatAndLon(@Param("lat") Float lat, @Param("lon") Float lon);

    // Проверка существования локации по координатам
    Boolean existsByLatAndLon(Float lat, Float lon);

    // Поиск локации по ID (уже есть в JpaRepository, но можно добавить для явности)
    Optional<Location> findById(Long id);
}
