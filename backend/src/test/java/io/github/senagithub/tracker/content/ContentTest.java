package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Content.update reports whether anything changed, so an unchanged save writes no UPDATED entry.
 * Plain objects, no Spring context and no DB; client ids are set by reflection, as the DB would.
 */
class ContentTest {

    private static final LocalDate DUE = LocalDate.of(2026, 11, 25);

    private Client client;
    private Content content;

    private static Client clientWithId(long id) {
        Client client = new Client("Otel " + id, "Otelcilik", "Ayşe Kaya", "ayse@example.com", null);
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    @BeforeEach
    void setUp() {
        client = clientWithId(1);
        content = new Content(client, "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram",
                ContentStatus.DRAFT, DUE, "Not");
    }

    @Test
    void sameValuesAreNoChange() {
        assertFalse(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE, "Not"));
    }

    @Test
    void sameClientIdInAnotherInstanceIsNoChange() {
        // The service loads the client again for each request, so it is compared by id, not identity.
        assertFalse(content.update(clientWithId(1), "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE, "Not"));
    }

    @Test
    void sameNullOptionalFieldsAreNoChange() {
        content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, null, DUE, null);
        assertFalse(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, null, DUE, null));
    }

    @Test
    void anotherClientIsAChange() {
        Client other = clientWithId(2);
        assertTrue(content.update(other, "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE, "Not"));
        assertEquals(other, content.getClient());
    }

    @Test
    void titleChangeIsAChange() {
        assertTrue(content.update(client, "Kış kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE, "Not"));
    }

    @Test
    void typeChangeIsAChange() {
        assertTrue(content.update(client, "Yaz kampanyası", ContentType.BLOG_POST, "Instagram", DUE, "Not"));
    }

    @Test
    void channelChangeIsAChange() {
        assertTrue(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, "TikTok", DUE, "Not"));
    }

    @Test
    void channelRemovedIsAChange() {
        assertTrue(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, null, DUE, "Not"));
    }

    @Test
    void dueDateChangeIsAChange() {
        assertTrue(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE.plusDays(1), "Not"));
    }

    @Test
    void notesChangeIsAChange() {
        assertTrue(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE, "Yeni not"));
    }

    @Test
    void notesRemovedIsAChange() {
        assertTrue(content.update(client, "Yaz kampanyası", ContentType.SOCIAL_POST, "Instagram", DUE, null));
    }

    @Test
    void statusIsNotTouched() {
        content.update(client, "Kış kampanyası", ContentType.BLOG_POST, null, DUE.plusDays(3), null);
        assertEquals(ContentStatus.DRAFT, content.getStatus());
    }
}
