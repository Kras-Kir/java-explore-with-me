package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.model.enums.RequestStatus;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotNull(message = "ID запросов не могут быть null")
    private List<Long> requestIds;

    @NotNull(message = "Статус не может быть null")
    private RequestStatus status;
}
