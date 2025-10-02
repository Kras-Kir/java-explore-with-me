package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentShortDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.model.enums.CommentStatus;
import ru.practicum.model.enums.EventState;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    // Публичные методы

    @Override
    public List<CommentShortDto> getPublishedCommentsForEvent(Long eventId, int from, int size) {
        log.info("Getting published comments for event id: {}", eventId);

        Event event = getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя получить комментарии для неопубликованного события");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByEventIdAndStatusOrderByCreatedOnDesc(
                eventId, CommentStatus.APPROVED, pageable);

        return comments.stream()
                .map(commentMapper::toCommentShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto getPublishedComment(Long commentId) {
        log.info("Getting published comment id: {}", commentId);

        Comment comment = getCommentById(commentId);

        if (comment.getStatus() != CommentStatus.APPROVED) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не найден");
        }

        return commentMapper.toCommentDto(comment);
    }

    // Приватные методы

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Creating comment by user id: {} for event id: {}", userId, eventId);

        User author = getUserById(userId);

        Event event = getEventById(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        // Проверяем, не комментировал ли уже пользователь это событие
        if (commentRepository.existsByAuthorIdAndEventId(userId, eventId)) {
            throw new ConflictException("Пользователь уже оставлял комментарий к этому событию");
        }

        Comment comment = commentMapper.toCommentWithEntities(newCommentDto, event, author);
        Comment savedComment = commentRepository.save(comment);

        log.info("Comment created with id: {}", savedComment.getId());
        return commentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Updating comment id: {} by user id: {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " от пользователя с id=" + userId + " не найден"));

        // При редактировании статус сбрасывается на модерацию
        comment.setStatus(CommentStatus.PENDING);
        commentMapper.updateCommentFromDto(updateCommentDto, comment);

        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment id: {} updated by user id: {}", commentId, userId);

        return commentMapper.toCommentDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Deleting comment id: {} by user id: {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " от пользователя с id=" + userId + " не найден"));

        commentRepository.delete(comment);
        log.info("Comment id: {} deleted by user id: {}", commentId, userId);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        log.info("Getting comments for user id: {}", userId);

        getUserById(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        return comments.stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    // Административные методы

    @Override
    public List<CommentDto> getCommentsForAdmin(List<Long> users, List<Long> events, String status,
                                                String rangeStart, String rangeEnd, int from, int size) {
        log.info("Getting comments for admin with filters");

        CommentStatus commentStatus = null;
        if (status != null) {
            try {
                commentStatus = CommentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Неизвестный статус: " + status);
            }
        }

        LocalDateTime start = rangeStart != null ? LocalDateTime.parse(rangeStart) : LocalDateTime.now().minusYears(100);
        LocalDateTime end = rangeEnd != null ? LocalDateTime.parse(rangeEnd) : LocalDateTime.now().plusYears(100);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findCommentsByAdmin(users, events, commentStatus, start, end, pageable);

        return comments.stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, String status) {
        log.info("Moderating comment id: {} with status: {}", commentId, status);

        Comment comment = getCommentById(commentId);

        CommentStatus newStatus;
        try {
            newStatus = CommentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный статус: " + status);
        }

        comment.setStatus(newStatus);
        comment.setUpdatedOn(LocalDateTime.now());

        Comment moderatedComment = commentRepository.save(comment);
        log.info("Comment id: {} moderated with status: {}", commentId, status);

        return commentMapper.toCommentDto(moderatedComment);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        log.info("Deleting comment id: {} by admin", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий с id=" + commentId + " не найден");
        }

        commentRepository.deleteById(commentId);
        log.info("Comment id: {} deleted by admin", commentId);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }

    private Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));
    }
}
