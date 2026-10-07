package com.opencircle.banter;

import com.opencircle.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

// A short public post on the shared board.
@Entity
@Table(name = "banters")
class Banter {

    static final int MAX_CONTENT_LENGTH = 280;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(nullable = false, length = 160)
    private String campus;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Banter() {
    }

    Banter(AppUser author, String content, Instant createdAt) {
        if (author == null) {
            throw new IllegalArgumentException("Author is required");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        this.author = author;
        this.campus = author.getCampus();
        this.content = normalizeContent(content);
        this.createdAt = createdAt;
    }

    static String normalizeContent(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Content is required");
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content must not exceed 280 characters");
        }

        return normalized;
    }

    UUID getId() {
        return id;
    }

    AppUser getAuthor() {
        return author;
    }

    String getCampus() {
        return campus;
    }

    String getContent() {
        return content;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
