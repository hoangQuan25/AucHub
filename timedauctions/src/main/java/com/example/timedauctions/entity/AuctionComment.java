package com.example.timedauctions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List; // THÊM VÀO
import java.util.UUID;

@Entity
@Table(name = "timed_auction_comments", schema = "timed_auction_schema", indexes = {
        @Index(name = "idx_comment_auction_parent_time", columnList = "timedAuctionId, parentId, createdAt ASC")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID timedAuctionId;

    @Column(name = "parentId")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentId", insertable = false, updatable = false)
    private AuctionComment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuctionComment> replies;


    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String usernameSnapshot;

    @Lob
    @Column(nullable = false)
    private String commentText;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}