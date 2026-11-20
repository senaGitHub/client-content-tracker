package io.github.senagithub.tracker.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String industry;

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;

    /** One of {@link CoverImages#ALL}, or null when the client has no cover. */
    @Column(name = "cover_image_url", length = 255)
    private String coverImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Client() {
        // required by JPA
    }

    public Client(String name, String industry, String contactName, String contactEmail, String coverImageUrl) {
        this.name = name;
        this.industry = industry;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.coverImageUrl = coverImageUrl;
    }

    /** Replaces the editable fields; the service trims them and checks the cover first. */
    public void update(String name, String industry, String contactName, String contactEmail, String coverImageUrl) {
        this.name = name;
        this.industry = industry;
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.coverImageUrl = coverImageUrl;
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

    public String getName() {
        return name;
    }

    public String getIndustry() {
        return industry;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
