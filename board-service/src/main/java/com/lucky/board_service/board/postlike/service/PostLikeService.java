package com.lucky.board_service.board.postlike.service;

import com.lucky.board_service.board.postlike.domain.PostLike;
import com.lucky.board_service.board.postlike.domain.PostLikeRepository;
import com.lucky.board_service.board.postlike.dto.PostLikeResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;

    @Autowired
    public PostLikeService(PostLikeRepository postLikeRepository) {
        this.postLikeRepository = postLikeRepository;
    }

    public PostLikeResponseDto like(String memberId, Long postId) {
        Long mId = Long.valueOf(memberId);
        if(postLikeRepository.findByPostIdAndMemberId(postId, mId).isPresent()) {
            throw new IllegalStateException("이미 좋아요를 눌렀습니다.");
        }
        PostLike postLike = new PostLike(mId, postId);
        postLikeRepository.save(postLike);
        return new PostLikeResponseDto(mId, postId);
    }

    public PostLikeResponseDto unLike(String memberId, Long postId) {
        Long mId = Long.valueOf(memberId);
        PostLike postLike = postLikeRepository.findByPostIdAndMemberId(postId, mId)
                .orElseThrow(() -> new IllegalStateException("좋아요를 누른 적이 없습니다."));

        postLikeRepository.delete(postLike);
        return new PostLikeResponseDto(mId, postId);
    }
}