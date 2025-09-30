package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Location;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT l FROM Location l WHERE l.lat = :lat AND l.lon = :lon")
    Optional<Location> findByLatAndLon(@Param("lat") Float lat, @Param("lon") Float lon);

    Optional<Location> findById(Long id);
}
