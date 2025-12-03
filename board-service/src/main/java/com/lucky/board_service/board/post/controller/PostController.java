package com.lucky.board_service.board.post.controller;

import com.lucky.board_service.board.post.domain.Post;
import com.lucky.board_service.board.post.dto.PostCreateRequestDto;
import com.lucky.board_service.board.post.dto.PostUpdateRequestDto;
import com.lucky.board_service.board.post.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/board-service/api")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Post> selectPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody PostCreateRequestDto requestDto) {
        return ResponseEntity.ok(postService.save(requestDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@RequestBody PostUpdateRequestDto requestDto, @PathVariable Long id) {
        return ResponseEntity.ok(postService.update(requestDto, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Post> deletePost(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.ok(null);
    }
}
