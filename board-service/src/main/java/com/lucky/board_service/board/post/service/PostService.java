package com.lucky.board_service.board.post.service;

import com.lucky.board_service.board.post.domain.Post;
import com.lucky.board_service.board.post.domain.PostRepository;
import com.lucky.board_service.board.post.dto.PostCreateRequestDto;
import com.lucky.board_service.board.post.dto.PostResponseDto;
import com.lucky.board_service.board.post.dto.PostUpdateRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private final PostRepository postRepository;

    @Autowired
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public PostResponseDto save(String username, PostCreateRequestDto requestDto) {
        Post post = new Post();

        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());
        post.setAuthor(username);

        postRepository.save(post);

        return new PostResponseDto(post);
    }

    public PostResponseDto findById(Long id) {
        Post post = postRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        return new PostResponseDto(post);
    }

    public PostResponseDto update(String username, PostUpdateRequestDto requestDto, Long id) {
        Post post = postRepository.findById(id).orElseThrow(IllegalStateException::new);

        if(!post.getAuthor().equals(username)){
            throw new IllegalArgumentException("작성자만 수정할 수 있습니다.");
        }

        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());
        postRepository.save(post);
        return new PostResponseDto(post);
    }

    public void delete(String username, Long id) {
        Post post = postRepository.findById(id).orElseThrow(IllegalArgumentException::new);

        if(!post.getAuthor().equals(username)){
            throw new IllegalArgumentException("작성자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }
}