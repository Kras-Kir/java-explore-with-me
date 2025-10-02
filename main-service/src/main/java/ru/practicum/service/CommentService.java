package ru.practicum.service;

import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentShortDto;
import ru.practicum.dto.NewCommentDto;

import java.util.List;

public interface CommentService {

    // Публичные методы
    List<CommentShortDto> getPublishedCommentsForEvent(Long eventId, int from, int size);

    CommentDto getPublishedComment(Long commentId);

    // Приватные методы (для авторизованных пользователей)
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto);

    void deleteComment(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    // Административные методы
    List<CommentDto> getCommentsForAdmin(List<Long> users, List<Long> events, String status,
                                         String rangeStart, String rangeEnd, int from, int size);

    CommentDto moderateComment(Long commentId, String status);

    void deleteCommentByAdmin(Long commentId);

}
