package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.CommentShortDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "status", expression = "java(ru.practicum.model.enums.CommentStatus.PENDING)")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedOn", ignore = true)
    Comment toComment(NewCommentDto newCommentDto);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "authorName", source = "author.name")
    CommentDto toCommentDto(Comment comment);

    @Mapping(target = "authorName", source = "author.name")
    CommentShortDto toCommentShortDto(Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", expression = "java(java.time.LocalDateTime.now())")
    void updateCommentFromDto(NewCommentDto newCommentDto, @MappingTarget Comment comment);

    default Comment toCommentWithEntities(NewCommentDto newCommentDto, Event event, User author) {
        Comment comment = toComment(newCommentDto);
        comment.setEvent(event);
        comment.setAuthor(author);
        return comment;
    }
}
