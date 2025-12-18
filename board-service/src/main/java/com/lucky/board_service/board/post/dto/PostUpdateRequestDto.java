package com.lucky.board_service.board.post.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostUpdateRequestDto {

    private String title;
    private String content;

    private List<String> hashTags;
}
