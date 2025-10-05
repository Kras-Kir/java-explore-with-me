package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CommentDto;
import ru.practicum.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class CommentAdminController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsForAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<Long> events,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /admin/comments - поиск комментариев с фильтрами");
        return commentService.getCommentsForAdmin(users, events, status, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{commentId}")
    public CommentDto moderateComment(
            @PathVariable Long commentId,
            @RequestParam String status) {

        log.info("PATCH /admin/comments/{} - модерация комментария со статусом {}", commentId, status);
        return commentService.moderateComment(commentId, status);
    }

    @DeleteMapping("/{commentId}")
    public void deleteCommentByAdmin(@PathVariable Long commentId) {
        log.info("DELETE /admin/comments/{} - удаление комментария администратором", commentId);
        commentService.deleteCommentByAdmin(commentId);
    }
}
