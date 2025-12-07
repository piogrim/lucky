package com.lucky.board_service.board.comment.controller;

import com.lucky.board_service.board.comment.dto.CommentCreateRequestDto;
import com.lucky.board_service.board.comment.dto.CommentResponseDto;
import com.lucky.board_service.board.comment.dto.CommentUpdateRequestDto;
import com.lucky.board_service.board.comment.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponseDto>createComment(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long postId,
            @RequestBody CommentCreateRequestDto requestDto) {

        return ResponseEntity.ok(commentService.saveComment(username, postId, requestDto));
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> getComment(
            @PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getComment(commentId));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequestDto requestDto
    ){
        return ResponseEntity.ok(commentService.updateComment(username, commentId, requestDto));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("X-User-Name") String username,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(username, commentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Page<CommentResponseDto>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, page));
    }
}
