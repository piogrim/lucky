package com.lucky.board_service.board.comment.service;

import com.lucky.board_service.board.comment.domain.Comment;
import com.lucky.board_service.board.comment.domain.CommentRepository;
import com.lucky.board_service.board.comment.dto.CommentCreateRequestDto;
import com.lucky.board_service.board.comment.dto.CommentResponseDto;
import com.lucky.board_service.board.comment.dto.CommentUpdateRequestDto;
import com.lucky.board_service.board.post.domain.Post;
import com.lucky.board_service.board.post.domain.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public CommentResponseDto saveComment(String username, Long postId, CommentCreateRequestDto requestDto) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시물이 더 이상 존재하지 않습니다."));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(username);
        comment.setContent(requestDto.getContent());
        Comment saveComment = commentRepository.save(comment);

        return new CommentResponseDto(saveComment);
    }

    public CommentResponseDto getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다."));
        return new CommentResponseDto(comment);
    }

    @Transactional
    public CommentResponseDto updateComment(String username, Long commentId, CommentUpdateRequestDto requestDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다."));

        if(!comment.getAuthor().equals(username)) {
            throw new IllegalStateException("작성자만 수정할 수 있습니다.");
        }

        comment.setContent(requestDto.getContent());
        Comment updateComment = commentRepository.save(comment);
        return new CommentResponseDto(updateComment);
    }

    public Page<CommentResponseDto> getCommentsByPost(Long postId, int pageNo) {
        Pageable pageable = PageRequest.of(pageNo, 50, Sort.by(Sort.Direction.DESC, "id"));

        Page<Comment> commentPage = commentRepository.findAllByPostId(postId, pageable);

        return commentPage.map(CommentResponseDto::new);
    }

    @Transactional
    public void deleteComment(String username, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                        .orElseThrow(() -> new IllegalStateException("해당 댓글이 존재하지 않습니다."));

        if(!comment.getAuthor().equals(username)) {
            throw new IllegalStateException("작성자만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }
}
