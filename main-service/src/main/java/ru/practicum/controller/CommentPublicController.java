package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentShortDto;
import ru.practicum.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentPublicController {

    private final CommentService commentService;

    @GetMapping("/events/{eventId}")
    public List<CommentShortDto> getPublishedCommentsForEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /comments/events/{} - получение опубликованных комментариев события", eventId);
        return commentService.getPublishedCommentsForEvent(eventId, from, size);
    }

    @GetMapping("/{commentId}")
    public CommentDto getPublishedComment(@PathVariable Long commentId) {
        log.info("GET /comments/{} - получение опубликованного комментария", commentId);
        return commentService.getPublishedComment(commentId);
    }
}
