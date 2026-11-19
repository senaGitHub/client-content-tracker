package io.github.senagithub.tracker.activity;

import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private ContentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private ContentStatus toStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityLog() {
        // required by JPA
    }

    public ActivityLog(Content content, ActivityAction action,
                       ContentStatus fromStatus, ContentStatus toStatus) {
        this.content = content;
        this.action = action;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    /** Only sets the timestamp when none was given, so the demo seeder can backdate records. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public ActivityAction getAction() {
        return action;
    }

    public ContentStatus getFromStatus() {
        return fromStatus;
    }

    public ContentStatus getToStatus() {
        return toStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
