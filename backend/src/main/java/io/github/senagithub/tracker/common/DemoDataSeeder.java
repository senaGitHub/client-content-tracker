package io.github.senagithub.tracker.common;

import io.github.senagithub.tracker.activity.ActivityAction;
import io.github.senagithub.tracker.activity.ActivityLog;
import io.github.senagithub.tracker.activity.ActivityLogRepository;
import io.github.senagithub.tracker.client.Client;
import io.github.senagithub.tracker.client.ClientRepository;
import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.content.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static io.github.senagithub.tracker.content.ContentStatus.DRAFT;
import static io.github.senagithub.tracker.content.ContentStatus.IN_REVIEW;
import static io.github.senagithub.tracker.content.ContentStatus.PUBLISHED;
import static io.github.senagithub.tracker.content.ContentType.BLOG_POST;
import static io.github.senagithub.tracker.content.ContentType.SOCIAL_POST;
import static io.github.senagithub.tracker.content.ContentType.WEB_PAGE;

/**
 * Fills an empty database with fictional demo data.
 * <p>
 * All dates are relative to today, so "overdue" and "due this week" always look realistic,
 * no matter when the app is started. The data is fixed (no randomness), so every fresh
 * database looks the same. Disable with {@code app.seed.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private record ClientSeed(String name, String industry, String contactName, String contactEmail,
                              List<ContentSeed> contents) {
    }

    /** {@code dueInDays} is relative to today; negative means the due date is in the past. */
    private record ContentSeed(String title, ContentType type, String channel, ContentStatus status,
                               int dueInDays, String notes) {
    }

    private static final List<ClientSeed> SEED = List.of(
            new ClientSeed("Turkuaz Bay Bodrum", "Boutique hotel", "Deniz Aksoy", "deniz.aksoy@turkuazbay.example", List.of(
                    new ContentSeed("Sunset dinner on the jetty", SOCIAL_POST, "Instagram", PUBLISHED, -6, null),
                    new ContentSeed("Early summer booking campaign", SOCIAL_POST, "Instagram", IN_REVIEW, -2,
                            "Waiting for final discount rate from revenue team."),
                    new ContentSeed("10 hidden coves near Bodrum", BLOG_POST, "Website", DRAFT, 9, null),
                    new ContentSeed("Rooms & suites page refresh", WEB_PAGE, "Website", IN_REVIEW, 3,
                            "New photos from the October shoot."),
                    new ContentSeed("Boat tour reel", SOCIAL_POST, "TikTok", DRAFT, 1, null),
                    new ContentSeed("Wedding packages landing page", WEB_PAGE, "Website", PUBLISHED, -12, null))),
            new ClientSeed("Peri Cave Hotel", "Cave hotel", "Elif Kaya", "elif.kaya@pericave.example", List.of(
                    new ContentSeed("Balloon morning from the terrace", SOCIAL_POST, "Instagram", PUBLISHED, -4, null),
                    new ContentSeed("A guide to Cappadocia's valleys", BLOG_POST, "Website", IN_REVIEW, 5, null),
                    new ContentSeed("Winter rates announcement", SOCIAL_POST, "Facebook", DRAFT, -3,
                            "Rates confirmed, copy still missing."),
                    new ContentSeed("Cave suite virtual tour page", WEB_PAGE, "Website", DRAFT, 14, null),
                    new ContentSeed("Pottery workshop in Avanos", BLOG_POST, "Website", PUBLISHED, -15, null),
                    new ContentSeed("New Year's Eve dinner teaser", SOCIAL_POST, "Instagram", IN_REVIEW, 2, null))),
            new ClientSeed("Lara Sands Resort", "Beach resort", "Mert Yıldız", "mert.yildiz@larasands.example", List.of(
                    new ContentSeed("Kids club summer program", WEB_PAGE, "Website", PUBLISHED, -9, null),
                    new ContentSeed("All-inclusive, explained", BLOG_POST, "Website", DRAFT, 6, null),
                    new ContentSeed("Poolside DJ night", SOCIAL_POST, "Instagram", IN_REVIEW, 0, null),
                    new ContentSeed("Family room comparison page", WEB_PAGE, "Website", DRAFT, 18, null),
                    new ContentSeed("Behind the scenes: our chefs", SOCIAL_POST, "YouTube", PUBLISHED, -2, null),
                    new ContentSeed("Early booking offer for next season", SOCIAL_POST, "Instagram", DRAFT, -1,
                            "Legal needs to approve the terms."))),
            new ClientSeed("Olive & Stone Spa", "Spa & wellness", "Zeynep Arslan", "zeynep.arslan@olivestone.example", List.of(
                    new ContentSeed("Hammam ritual, step by step", BLOG_POST, "Website", PUBLISHED, -7, null),
                    new ContentSeed("Winter detox packages page", WEB_PAGE, "Website", IN_REVIEW, 4, null),
                    new ContentSeed("Therapist spotlight", SOCIAL_POST, "Instagram", DRAFT, 3, null),
                    new ContentSeed("Couples spa day offer", SOCIAL_POST, "Facebook", PUBLISHED, 2,
                            "Scheduled ahead of the weekend."),
                    new ContentSeed("Olive oil skincare: myths and facts", BLOG_POST, "Website", DRAFT, 11, null),
                    new ContentSeed("Gift card launch post", SOCIAL_POST, "Instagram", IN_REVIEW, -4,
                            "Client asked for a second visual option."))),
            new ClientSeed("Antalya Travel Co.", "Travel agency", "Can Demir", "can.demir@antalyatravel.example", List.of(
                    new ContentSeed("Old Town walking tour", BLOG_POST, "Website", PUBLISHED, -10, null),
                    new ContentSeed("Day trips from Antalya", WEB_PAGE, "Website", DRAFT, 8, null),
                    new ContentSeed("Düden waterfalls carousel", SOCIAL_POST, "Instagram", IN_REVIEW, 1, null),
                    new ContentSeed("A week on the Lycian Way", BLOG_POST, "Website", DRAFT, 20, null),
                    new ContentSeed("Tour booking FAQ page", WEB_PAGE, "Website", PUBLISHED, 4, null),
                    new ContentSeed("Weekend getaway deals", SOCIAL_POST, "Facebook", DRAFT, 5, null))),
            new ClientSeed("Medina Health", "Health tourism", "Ayşe Çelik", "ayse.celik@medinahealth.example", List.of(
                    new ContentSeed("Patient journey video", SOCIAL_POST, "YouTube", IN_REVIEW, 6, null),
                    new ContentSeed("Dental treatment packages page", WEB_PAGE, "Website", PUBLISHED, -8, null),
                    new ContentSeed("Recovery after hair transplant: what to expect", BLOG_POST, "Website", IN_REVIEW, -1,
                            "Medical team is reviewing the claims."),
                    new ContentSeed("Meet our international patient team", SOCIAL_POST, "LinkedIn", DRAFT, 12, null),
                    new ContentSeed("Accreditation announcement", SOCIAL_POST, "LinkedIn", PUBLISHED, -3, null),
                    new ContentSeed("Partner recovery hotels page", WEB_PAGE, "Website", DRAFT, 2, null)))
    );

    private final ClientRepository clientRepository;
    private final ContentRepository contentRepository;
    private final ActivityLogRepository activityLogRepository;

    public DemoDataSeeder(ClientRepository clientRepository,
                          ContentRepository contentRepository,
                          ActivityLogRepository activityLogRepository) {
        this.clientRepository = clientRepository;
        this.contentRepository = contentRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clientRepository.count() > 0) {
            log.info("Demo data skipped: database already has clients.");
            return;
        }

        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);

        int contentIndex = 0;
        int activityCount = 0;
        for (int c = 0; c < SEED.size(); c++) {
            ClientSeed clientSeed = SEED.get(c);
            Client client = new Client(clientSeed.name(), clientSeed.industry(),
                    clientSeed.contactName(), clientSeed.contactEmail());
            client.setCreatedAt(now.minus(Duration.ofDays(40 - c)));
            clientRepository.save(client);

            for (ContentSeed seed : clientSeed.contents()) {
                LocalDate dueDate = today.plusDays(seed.dueInDays());
                Instant dueMorning = dueDate.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant();

                // Created two weeks before the due date, but never later than about a day ago.
                // The per-item offset keeps timestamps distinct so "recent activity" has a stable order.
                Instant latestCreation = now.minus(Duration.ofHours(24 + contentIndex * 2L));
                Instant createdAt = min(dueMorning.minus(Duration.ofDays(14)), latestCreation);

                // The content is stored with its final status directly; the activity log below
                // replays the history that would have led there (DRAFT -> IN_REVIEW -> PUBLISHED).
                Instant end = seed.status() == PUBLISHED ? min(dueMorning, now) : now;
                Instant lastChange = createdAt;
                if (seed.status() == IN_REVIEW || seed.status() == PUBLISHED) {
                    lastChange = between(createdAt, end, 0.5);
                }
                if (seed.status() == PUBLISHED) {
                    lastChange = between(createdAt, end, 0.9);
                }

                Content content = new Content(client, seed.title(), seed.type(), seed.channel(),
                        seed.status(), dueDate, seed.notes());
                content.setCreatedAt(createdAt);
                content.setUpdatedAt(lastChange);
                contentRepository.save(content);

                activityCount += addActivity(content, ActivityAction.CREATED, null, null, createdAt);
                if (seed.status() == IN_REVIEW || seed.status() == PUBLISHED) {
                    activityCount += addActivity(content, ActivityAction.STATUS_CHANGED, DRAFT, IN_REVIEW,
                            between(createdAt, end, 0.5));
                }
                if (seed.status() == PUBLISHED) {
                    activityCount += addActivity(content, ActivityAction.STATUS_CHANGED, IN_REVIEW, PUBLISHED,
                            between(createdAt, end, 0.9));
                }
                contentIndex++;
            }
        }

        log.info("Demo data seeded: {} clients, {} contents, {} activity entries.",
                SEED.size(), contentIndex, activityCount);
    }

    private int addActivity(Content content, ActivityAction action, ContentStatus from, ContentStatus to, Instant at) {
        ActivityLog entry = new ActivityLog(content, action, from, to);
        entry.setCreatedAt(at);
        activityLogRepository.save(entry);
        return 1;
    }

    private static Instant between(Instant start, Instant end, double fraction) {
        long millis = Duration.between(start, end).toMillis();
        return start.plusMillis(Math.max(0, (long) (millis * fraction)));
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
