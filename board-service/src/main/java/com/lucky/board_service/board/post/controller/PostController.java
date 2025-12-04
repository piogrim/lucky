package com.lucky.board_service.board.post.controller;

import com.lucky.board_service.board.post.dto.PostCreateRequestDto;
import com.lucky.board_service.board.post.dto.PostResponseDto;
import com.lucky.board_service.board.post.dto.PostUpdateRequestDto;
import com.lucky.board_service.board.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> selectPost(
            @PathVariable Long id) {
        return ResponseEntity.ok(postService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PostResponseDto> createPost(
            @RequestHeader("X-User-Name") String username,
            @RequestBody PostCreateRequestDto requestDto) {
        return ResponseEntity.ok(postService.save(username, requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostResponseDto> updatePost(
            @RequestHeader("X-User-Name") String username,
            @RequestBody PostUpdateRequestDto requestDto,
            @PathVariable Long id) {
        return ResponseEntity.ok(postService.update(username, requestDto, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long id) {
        postService.delete(username, id);
        return ResponseEntity.ok(null);
    }
}
