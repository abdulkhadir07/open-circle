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

@Entity
@Table(name = "banter_replies")
class BanterReply {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "banter_id", nullable = false)
    private Banter banter;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(nullable = false, length = Banter.MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BanterReply() {
    }

    BanterReply(Banter banter, AppUser author, String content, Instant createdAt) {
        if (banter == null) {
            throw new IllegalArgumentException("Banter is required");
        }

        if (author == null) {
            throw new IllegalArgumentException("Author is required");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        this.banter = banter;
        this.author = author;
        this.content = Banter.normalizeContent(content);
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    Banter getBanter() {
        return banter;
    }

    AppUser getAuthor() {
        return author;
    }

    String getContent() {
        return content;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
