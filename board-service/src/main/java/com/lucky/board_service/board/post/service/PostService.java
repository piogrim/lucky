package com.lucky.board_service.board.post.service;

import com.lucky.board_service.board.post.domain.Post;
import com.lucky.board_service.board.post.domain.PostRepository;
import com.lucky.board_service.board.post.dto.PostCreateRequestDto;
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

    public Post save(PostCreateRequestDto requestDto) {
        Post post = new Post();

        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());

        return postRepository.save(post);
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElseThrow(IllegalStateException::new);
    }

    public Post update(PostUpdateRequestDto requestDto, Long id) {
        Post post = postRepository.findById(id).orElseThrow(IllegalStateException::new);
        post.setTitle(requestDto.getTitle());
        post.setContent(requestDto.getContent());
        return postRepository.save(post);
    }

    public void delete(Long id) {
        postRepository.deleteById(id);
    }
}