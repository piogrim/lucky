package com.lucky.board_service.board.postlike.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "post_like",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_member",
                        columnNames = {"post_id", "member_id"}
                )
        }
)
@NoArgsConstructor
public class PostLike {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public PostLike(Long memberId, Long postId) {
        this.postId = postId;
        this.memberId = memberId;
    }
}
