package ru.practicum.validator;

import org.springframework.stereotype.Component;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;

@Component
public class DateValidator {

    public void validateEventDate(LocalDateTime eventDate, int hoursBefore) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(hoursBefore))) {
            throw new ValidationException(
                    String.format("Дата события должна быть не ранее чем за %d часа от текущего момента", hoursBefore)
            );
        }
    }

    public void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания");
        }
    }

    public void validatePaginationParams(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new ValidationException("Параметр 'from' не может быть отрицательным");
        }
        if (size == null || size <= 0) {
            throw new ValidationException("Параметр 'size' должен быть положительным");
        }
    }
}
