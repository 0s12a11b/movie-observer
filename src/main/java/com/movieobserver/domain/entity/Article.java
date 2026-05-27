package com.movieobserver.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Article {

    public enum ArticleCategory {
        NEWS,
        REVIEW,
        INTERVIEW,
        FEATURE,
        ANALYSIS,
        RETROSPECTIVE
    }

    public enum ArticleStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "lead_paragraph", nullable = false, columnDefinition = "TEXT")
    private String leadParagraph;

    @Column(name = "body_content", nullable = false, columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArticleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArticleStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    @ToString.Exclude
    private Movie movie;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
