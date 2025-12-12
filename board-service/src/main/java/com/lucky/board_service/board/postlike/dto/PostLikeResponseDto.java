package com.lucky.board_service.board.postlike.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostLikeResponseDto {

    private Long postId;

    private Long memberId;

    public PostLikeResponseDto(Long memberId, Long postId) {
        this.memberId = memberId;
        this.postId = postId;
    }
}
