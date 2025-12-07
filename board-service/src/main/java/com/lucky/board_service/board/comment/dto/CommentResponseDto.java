package com.lucky.board_service.board.comment.dto;

import com.lucky.board_service.board.comment.domain.Comment;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponseDto {

    private Long id;

    private String content;

    private String author;

    public CommentResponseDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.author = comment.getAuthor();
    }
}
