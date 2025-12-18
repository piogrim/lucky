package com.lucky.board_service.board.hashtag.service;

import com.lucky.board_service.board.hashtag.domain.HashTag;
import com.lucky.board_service.board.hashtag.domain.HashTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HashTagService {

    private final HashTagRepository hashTagRepository;

    @Autowired
    public HashTagService(final HashTagRepository hashTagRepository) {
        this.hashTagRepository = hashTagRepository;
    }

    public HashTag saveHashTag(String tagName) {
        return hashTagRepository.findByTag(tagName)
                .orElseGet(() -> hashTagRepository.save(new HashTag(tagName)));
    }
}
