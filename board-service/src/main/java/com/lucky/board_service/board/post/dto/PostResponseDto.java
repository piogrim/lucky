package com.lucky.board_service.board.post.dto;

import com.lucky.board_service.board.post.domain.Post;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostResponseDto {

    private Long id;
    private String author;
    private String title;
    private String content;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.author = post.getAuthor();
        this.title = post.getTitle();
        this.content = post.getContent();
    }
}
