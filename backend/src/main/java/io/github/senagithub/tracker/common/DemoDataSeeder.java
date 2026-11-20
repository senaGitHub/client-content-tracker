package io.github.senagithub.tracker.common;

import io.github.senagithub.tracker.activity.ActivityAction;
import io.github.senagithub.tracker.activity.ActivityLog;
import io.github.senagithub.tracker.activity.ActivityLogRepository;
import io.github.senagithub.tracker.client.Client;
import io.github.senagithub.tracker.client.ClientRepository;
import io.github.senagithub.tracker.client.CoverImages;
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

    private record ClientSeed(String name, String industry, String contactName, String contactEmail, String coverImageUrl,
                              List<ContentSeed> contents) {
    }

    /** {@code dueInDays} is relative to today; negative means the due date is in the past. */
    private record ContentSeed(String title, ContentType type, String channel, ContentStatus status,
                               int dueInDays, String notes) {
    }

    private static final String WEB = "Web sitesi";

    private static final List<ClientSeed> SEED = List.of(
            new ClientSeed("Turkuaz Bay Bodrum", "Butik otel", "Deniz Aksoy", "deniz.aksoy@turkuazbay.example",
                    CoverImages.INFINITY_POOL, List.of(
                    new ContentSeed("İskelede gün batımı yemeği", SOCIAL_POST, "Instagram", PUBLISHED, -6, null),
                    new ContentSeed("Erken yaz rezervasyon kampanyası", SOCIAL_POST, "Instagram", IN_REVIEW, -2,
                            "Gelir ekibinden son indirim oranı bekleniyor."),
                    new ContentSeed("Bodrum'un 10 gizli koyu", BLOG_POST, WEB, DRAFT, 9, null),
                    new ContentSeed("Odalar ve süitler sayfası yenileme", WEB_PAGE, WEB, IN_REVIEW, 3,
                            "Ekim çekiminden yeni fotoğraflar eklenecek."),
                    new ContentSeed("Tekne turu videosu", SOCIAL_POST, "TikTok", DRAFT, 1, null),
                    new ContentSeed("Düğün paketleri sayfası", WEB_PAGE, WEB, PUBLISHED, -12, null),
                    new ContentSeed("Sonbahar yoga haftası duyurusu", SOCIAL_POST, "Instagram", PUBLISHED, -5, null),
                    new ContentSeed("Bodrum marina gezi rehberi", BLOG_POST, "Blog", PUBLISHED, -11, null),
                    new ContentSeed("Spa ve masaj menüsü sayfası", WEB_PAGE, WEB, PUBLISHED, -16,
                            "Fiyatlar 2027 sezonu için güncellendi."),
                    new ContentSeed("Yılbaşı konaklama paketi", SOCIAL_POST, "Facebook", IN_REVIEW, 8,
                            "Fiyat tablosu ekte; görselde logo eksik."),
                    new ContentSeed("Şefin Ege mezeleri serisi", SOCIAL_POST, "Instagram", DRAFT, 4, null),
                    new ContentSeed("Kapalı sezonda Bodrum rehberi", BLOG_POST, WEB, DRAFT, 15, null))),
            new ClientSeed("Peri Cave Hotel", "Mağara otel", "Elif Kaya", "elif.kaya@pericave.example",
                    CoverImages.CAPPADOCIA_BALLOON, List.of(
                    new ContentSeed("Terastan balonlu sabah", SOCIAL_POST, "Instagram", PUBLISHED, -4, null),
                    new ContentSeed("Kapadokya vadileri rehberi", BLOG_POST, WEB, IN_REVIEW, 5, null),
                    new ContentSeed("Kış fiyatları duyurusu", SOCIAL_POST, "Facebook", DRAFT, -3,
                            "Fiyatlar onaylandı, metin henüz yazılmadı."),
                    new ContentSeed("Mağara süit sanal tur sayfası", WEB_PAGE, WEB, DRAFT, 14, null),
                    new ContentSeed("Avanos'ta çömlek atölyesi", BLOG_POST, WEB, PUBLISHED, -15, null),
                    new ContentSeed("Yılbaşı gala yemeği tanıtımı", SOCIAL_POST, "Instagram", IN_REVIEW, 2, null),
                    new ContentSeed("Ürgüp şarap tadım rotası", BLOG_POST, WEB, PUBLISHED, -9, null),
                    new ContentSeed("Mağara odada bir gece", SOCIAL_POST, "TikTok", PUBLISHED, -1, null),
                    new ContentSeed("Havalimanı transferi ve ulaşım sayfası", WEB_PAGE, WEB, PUBLISHED, -20, null),
                    new ContentSeed("Balon turu rezervasyon rehberi", WEB_PAGE, WEB, IN_REVIEW, 6,
                            "Tur firmasının iptal koşulları teyit edilecek."),
                    new ContentSeed("Karlı vadilerde kış sabahları", SOCIAL_POST, "Instagram", DRAFT, 10, null),
                    new ContentSeed("Köy kahvaltısı sofrasının hikâyesi", SOCIAL_POST, "Facebook", DRAFT, 3, null))),
            new ClientSeed("Lara Sands Resort", "Sahil resort", "Mert Yıldız", "mert.yildiz@larasands.example",
                    CoverImages.PALM_POOL, List.of(
                    new ContentSeed("Çocuk kulübü yaz programı", WEB_PAGE, WEB, PUBLISHED, -9, null),
                    new ContentSeed("Her şey dahil konsepti, adım adım", BLOG_POST, WEB, DRAFT, 6, null),
                    new ContentSeed("Havuz başı DJ gecesi", SOCIAL_POST, "Instagram", IN_REVIEW, 0, null),
                    new ContentSeed("Aile odaları karşılaştırma sayfası", WEB_PAGE, WEB, DRAFT, 18, null),
                    new ContentSeed("Kamera arkası: şeflerimiz", SOCIAL_POST, "YouTube", PUBLISHED, -2, null),
                    new ContentSeed("Gelecek sezon erken rezervasyon", SOCIAL_POST, "Instagram", DRAFT, 7,
                            "Kampanya koşulları hukuk onayında."),
                    new ContentSeed("Su parkı yeniden açılıyor", SOCIAL_POST, "Facebook", PUBLISHED, -13, null),
                    new ContentSeed("Havalimanından resort'a ulaşım", BLOG_POST, WEB, PUBLISHED, -6, null),
                    new ContentSeed("Animasyon ekibiyle bir gün", SOCIAL_POST, "YouTube", PUBLISHED, -3, null),
                    new ContentSeed("Kongre ve toplantı salonları sayfası", WEB_PAGE, WEB, IN_REVIEW, 9,
                            "Kapasite tablosu satış ekibinden geldi."),
                    new ContentSeed("Sahil boyunca koşu parkuru", SOCIAL_POST, "Instagram", DRAFT, 2, null),
                    new ContentSeed("Sezon sonu misafir bülteni", BLOG_POST, "E-bülten", DRAFT, 12, null))),
            new ClientSeed("Olive & Stone Spa", "Spa ve wellness", "Zeynep Arslan", "zeynep.arslan@olivestone.example",
                    CoverImages.SPA_POOL, List.of(
                    new ContentSeed("Adım adım hamam ritüeli", BLOG_POST, WEB, PUBLISHED, -7, null),
                    new ContentSeed("Kış detoks paketleri sayfası", WEB_PAGE, WEB, IN_REVIEW, 4, null),
                    new ContentSeed("Terapistimizle tanışın", SOCIAL_POST, "Instagram", DRAFT, 3, null),
                    new ContentSeed("Çiftlere özel spa günü", SOCIAL_POST, "Facebook", PUBLISHED, 2,
                            "Hafta sonundan önce yayına alındı."),
                    new ContentSeed("Zeytinyağlı cilt bakımı: doğrular ve yanlışlar", BLOG_POST, WEB, DRAFT, 11, null),
                    new ContentSeed("Hediye kartı lansmanı", SOCIAL_POST, "Instagram", IN_REVIEW, -4,
                            "Müşteri ikinci bir görsel seçeneği istedi."),
                    new ContentSeed("Aromaterapi yağları rehberi", BLOG_POST, "Blog", PUBLISHED, -12, null),
                    new ContentSeed("Gün doğumunda meditasyon seansları", SOCIAL_POST, "Instagram", PUBLISHED, -5, null),
                    new ContentSeed("Fiyat listesi ve randevu sayfası", WEB_PAGE, WEB, PUBLISHED, -18, null),
                    new ContentSeed("Yeni annelere yenilenme paketi", SOCIAL_POST, "Facebook", IN_REVIEW, 7,
                            "Görseldeki model izni yenilenecek."),
                    new ContentSeed("Kışın cilt bakımı için beş öneri", BLOG_POST, WEB, DRAFT, 9, null),
                    new ContentSeed("Kurumsal wellness günleri", SOCIAL_POST, "LinkedIn", DRAFT, 13, null))),
            new ClientSeed("Antalya Travel Co.", "Seyahat acentesi", "Can Demir", "can.demir@antalyatravel.example",
                    CoverImages.ANTALYA_CLIFFS, List.of(
                    new ContentSeed("Kaleiçi yürüyüş turu", BLOG_POST, WEB, PUBLISHED, -10, null),
                    new ContentSeed("Antalya'dan günübirlik turlar", WEB_PAGE, WEB, DRAFT, 8, null),
                    new ContentSeed("Düden Şelalesi karuseli", SOCIAL_POST, "Instagram", IN_REVIEW, 1, null),
                    new ContentSeed("Likya Yolu'nda bir hafta", BLOG_POST, WEB, DRAFT, 20, null),
                    new ContentSeed("Tur rezervasyonu SSS sayfası", WEB_PAGE, WEB, PUBLISHED, 4, null),
                    new ContentSeed("Hafta sonu kaçamak fırsatları", SOCIAL_POST, "Facebook", DRAFT, 5, null),
                    new ContentSeed("Perge ve Aspendos antik kent turu", BLOG_POST, WEB, PUBLISHED, -14, null),
                    new ContentSeed("Tekne turundan kareler", SOCIAL_POST, "Instagram", PUBLISHED, -2, null),
                    new ContentSeed("Kapadokya bağlantılı paket turlar sayfası", WEB_PAGE, WEB, IN_REVIEW, 10,
                            "Partner otelin kış fiyatları güncellenecek."),
                    new ContentSeed("Rafting günü için hazırlık listesi", BLOG_POST, "Blog", DRAFT, 6, null),
                    new ContentSeed("Gezginlerden yorumlar derlemesi", SOCIAL_POST, "Facebook", DRAFT, 4, null),
                    new ContentSeed("Kış dönemi tur takvimi sayfası", WEB_PAGE, WEB, DRAFT, 16, null))),
            new ClientSeed("Medina Health", "Sağlık turizmi", "Ayşe Çelik", "ayse.celik@medinahealth.example",
                    CoverImages.SEASIDE_YOGA, List.of(
                    new ContentSeed("Hasta yolculuğu videosu", SOCIAL_POST, "YouTube", IN_REVIEW, 6, null),
                    new ContentSeed("Diş tedavisi paketleri sayfası", WEB_PAGE, WEB, PUBLISHED, -8, null),
                    new ContentSeed("Saç ekimi sonrası iyileşme: neler beklenmeli", BLOG_POST, WEB, IN_REVIEW, 3,
                            "Tıbbi ekip iddiaları kontrol ediyor."),
                    new ContentSeed("Uluslararası hasta ekibimizle tanışın", SOCIAL_POST, "LinkedIn", DRAFT, 12, null),
                    new ContentSeed("Akreditasyon duyurusu", SOCIAL_POST, "LinkedIn", PUBLISHED, -3, null),
                    new ContentSeed("Anlaşmalı iyileşme otelleri sayfası", WEB_PAGE, WEB, DRAFT, 2, null),
                    new ContentSeed("Ücretsiz ön görüşme duyurusu", SOCIAL_POST, "Facebook", PUBLISHED, -6, null),
                    new ContentSeed("Göz tedavisi süreç sayfası", WEB_PAGE, WEB, PUBLISHED, -17, null),
                    new ContentSeed("Tedavi sonrası Antalya'da dinlenme", BLOG_POST, WEB, IN_REVIEW, 5,
                            "Hukuk ekibi sağlık beyanlarını inceliyor."),
                    new ContentSeed("Hasta hikâyesi: yurt dışından gelen bir aile", SOCIAL_POST, "YouTube", DRAFT, 11,
                            "Ailenin yazılı izni alındı."),
                    new ContentSeed("Sigorta ve ödeme hakkında sık sorulanlar", WEB_PAGE, WEB, DRAFT, 8, null),
                    new ContentSeed("Doktorlarımızla canlı soru-cevap", SOCIAL_POST, "Instagram", DRAFT, 3, null))),
            new ClientSeed("Fig & Thyme Bodrum", "Restoran", "Selin Öztürk", "selin.ozturk@figthyme.example",
                    CoverImages.OLIVE_TABLE, List.of(
                    new ContentSeed("Sonbahar tadım menüsü lansmanı", SOCIAL_POST, "Instagram", PUBLISHED, -8, null),
                    new ContentSeed("Ege otlarıyla beş tarif", BLOG_POST, "Blog", PUBLISHED, -14, null),
                    new ContentSeed("Rezervasyon ve özel etkinlik sayfası", WEB_PAGE, WEB, PUBLISHED, -19, null),
                    new ContentSeed("Şefle sabah pazarı", SOCIAL_POST, "YouTube", PUBLISHED, -3, null),
                    new ContentSeed("Zeytinyağı hasadı günlüğü", BLOG_POST, WEB, PUBLISHED, -6,
                            "Üretici çiftliğin adı metinden çıkarıldı."),
                    new ContentSeed("Yılbaşı akşam yemeği menüsü", SOCIAL_POST, "Instagram", IN_REVIEW, 4,
                            "Şarap eşleştirmesi sommelier onayında."),
                    new ContentSeed("Menü ve alerjen bilgisi sayfası", WEB_PAGE, WEB, IN_REVIEW, 7, null),
                    new ContentSeed("Mevsim sebzeleri: kasım listesi", SOCIAL_POST, "Facebook", DRAFT, -1,
                            "Fotoğraf çekimi hava yüzünden ertelendi."),
                    new ContentSeed("Mutfak ekibimizle tanışın", SOCIAL_POST, "Instagram", DRAFT, 5, null),
                    new ContentSeed("Kış akşamları için Ege tatlıları", BLOG_POST, WEB, DRAFT, 10, null),
                    new ContentSeed("Hediye kartı sayfası", WEB_PAGE, WEB, DRAFT, 14, null),
                    new ContentSeed("Pazar brunch'ı duyurusu", SOCIAL_POST, "Facebook", DRAFT, 2, null))),
            // No cover on purpose: the demo shows the initials fallback (brand card, hero, brand strip).
            new ClientSeed("Tuff Sky Balloons", "Balon turu", "Burak Şahin", "burak.sahin@tuffsky.example",
                    null, List.of(
                    new ContentSeed("Gün doğumu uçuşundan ilk izlenimler", SOCIAL_POST, "Instagram", PUBLISHED, -4, null),
                    new ContentSeed("Balon turu güvenliği: sık sorulanlar", WEB_PAGE, WEB, PUBLISHED, -21, null),
                    new ContentSeed("Pilotumuzla gökyüzünde 20 yıl", SOCIAL_POST, "YouTube", PUBLISHED, -9, null),
                    new ContentSeed("Balon uçuşunda ne giyilir?", BLOG_POST, "Blog", PUBLISHED, -12, null),
                    new ContentSeed("Kış uçuşları fiyat duyurusu", SOCIAL_POST, "Facebook", IN_REVIEW, 3,
                            "Rüzgâr nedeniyle iptal koşulları eklenecek."),
                    new ContentSeed("Özel uçuş ve evlilik teklifi paketi sayfası", WEB_PAGE, WEB, IN_REVIEW, 8, null),
                    new ContentSeed("Fotoğrafçılar için en iyi uçuş saatleri", BLOG_POST, WEB, IN_REVIEW, 6, null),
                    new ContentSeed("Yolcu yorumlarından seçmeler", SOCIAL_POST, "Instagram", DRAFT, 1, null),
                    new ContentSeed("Göreme otellerinden transfer sayfası", WEB_PAGE, WEB, DRAFT, 11, null),
                    new ContentSeed("Kamera arkası: balon nasıl şişirilir", SOCIAL_POST, "TikTok", DRAFT, 4, null),
                    new ContentSeed("Uçuş sertifikası tasarımı duyurusu", SOCIAL_POST, "Instagram", DRAFT, 9, null),
                    new ContentSeed("Kapadokya'da yıldız gözlemi rehberi", BLOG_POST, WEB, DRAFT, 17, null)))
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
                    clientSeed.contactName(), clientSeed.contactEmail(), clientSeed.coverImageUrl());
            // Clients joined 2–4 months ago, so "… tarihinden beri" on the brand page looks realistic.
            client.setCreatedAt(now.minus(Duration.ofDays(120 - c * 11L)));
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
