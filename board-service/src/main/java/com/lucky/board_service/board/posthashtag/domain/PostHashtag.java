package com.lucky.board_service.board.posthashtag.domain;

import com.lucky.board_service.board.hashtag.domain.HashTag;
import com.lucky.board_service.board.post.domain.Post;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "post_hashtag",
                        columnNames = {"post_id", "hashtag_id"}
                )
        }
)
public class PostHashtag {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id")
    private HashTag hashTag;

    public PostHashtag(Post post, HashTag hashTag) {
        this.post = post;
        this.hashTag = hashTag;
    }
}
