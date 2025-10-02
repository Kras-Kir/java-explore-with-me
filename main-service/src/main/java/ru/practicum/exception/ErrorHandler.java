package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.dto.ApiError;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(final NotFoundException e) {
        log.error("Объект не найден: {}", e.getMessage());
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Требуемый объект не был найден.")
                .reason("The required object was not found.")
                .status(HttpStatus.NOT_FOUND)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(final ConflictException e) {
        log.error("Конфликт данных: {}", e.getMessage());
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Нарушение целостности данных.")
                .reason("Integrity constraint has been violated.")
                .status(HttpStatus.CONFLICT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(final ValidationException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Некорректный запрос.")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(final BadRequestException e) {
        log.error("Некорректный запрос: {}", e.getMessage());
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Некорректный запрос.")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbiddenException(final ForbiddenException e) {
        log.error("Доступ запрещен: {}", e.getMessage());
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Для запрошенной операции условия не выполнены.")
                .reason("For the requested operation the conditions are not met.")
                .status(HttpStatus.FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.error("Ошибка валидации аргументов: {}", errors);
        return ApiError.builder()
                .errors(errors)
                .message("Некорректный запрос.")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        String error = String.format("Параметр '%s' имеет неверный тип. Значение: '%s'",
                e.getName(), e.getValue());

        log.error("Ошибка типа аргумента: {}", error);
        return ApiError.builder()
                .errors(List.of(error))
                .message("Некорректный запрос.")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameterException(final MissingServletRequestParameterException e) {
        String error = String.format("Отсутствует обязательный параметр: '%s'", e.getParameterName());

        log.error("Отсутствует обязательный параметр: {}", error);
        return ApiError.builder()
                .errors(List.of(error))
                .message("Некорректный запрос.")
                .reason("Incorrectly made request.")
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.error("Нарушение целостности данных: {}", e.getMessage());
        return ApiError.builder()
                .errors(List.of(e.getMessage()))
                .message("Нарушение целостности данных.")
                .reason("Integrity constraint has been violated.")
                .status(HttpStatus.CONFLICT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.toList()))
                .message("Внутренняя ошибка сервера.")
                .reason("Internal server error.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .timestamp(LocalDateTime.now())
                .build();
    }
}