package com.lucky.board_service.board.post.dto;

import com.lucky.board_service.board.post.domain.Post;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PostResponseDto {

    private Long id;
    private String author;
    private String title;
    private String content;
    private List<String> hashTags;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.author = post.getAuthor();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.hashTags = post.getPostHashtags().stream()
                .map(ph -> ph.getHashTag().getTag())
                .collect(Collectors.toList());
    }
}
