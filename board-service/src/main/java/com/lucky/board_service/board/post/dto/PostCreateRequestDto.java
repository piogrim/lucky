package com.lucky.board_service.board.post.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostCreateRequestDto {

    private String username;
    private String title;
    private String content;
}
