package com.lucky.board_service.board.postlike.controller;

import com.lucky.board_service.board.postlike.dto.PostLikeResponseDto;
import com.lucky.board_service.board.postlike.service.PostLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @Autowired
    public PostLikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<PostLikeResponseDto> like(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("id") Long postId) {
        return ResponseEntity.ok(postLikeService.like(userId, postId));
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<PostLikeResponseDto> unlike(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("id") Long postId){
        return ResponseEntity.ok(postLikeService.unLike(userId, postId));
    }
}
