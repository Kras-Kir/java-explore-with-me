package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Аннотация не может быть пустой")
    @Size(min = 20, max = 2000, message = "Аннотация должна содержать от 20 до 2000 символов")
    private String annotation;

    @NotNull(message = "Категория не может быть null")
    private Long category;

    @NotBlank(message = "Описание не может быть пустым")
    @Size(min = 20, max = 7000, message = "Описание должно содержать от 20 до 7000 символов")
    private String description;

    @NotNull(message = "Дата события не может быть null")
    @Future(message = "Дата события должна быть в будущем")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    @NotNull(message = "Локация не может быть null")
    private LocationDto location;

    private Boolean paid = false;

    @Min(value = 0, message = "Лимит участников не может быть отрицательным")
    private Integer participantLimit = 0;

    private Boolean requestModeration = true;

    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(min = 3, max = 120, message = "Заголовок должен содержать от 3 до 120 символов")
    private String title;
}
