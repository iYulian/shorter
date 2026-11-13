package ru.url.shorter.service;

import org.junit.jupiter.api.Test;
import ru.url.shorter.common.UriWrapper;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ShortenerUnitTest {

    private UriWrapper createUri() {
        return new UriWrapper(URI.create("https://example.org"), 0, 2, true,
                LocalDateTime.now().plusHours(1));
    }

    @Test
    void validatesHttpUrls() {
        assertTrue(URIShorterUtil.getHttpUrl("https://example.org/path?q=1#part").isPresent());
        assertTrue(URIShorterUtil.getHttpUrl("http://localhost:8080").isPresent());
        for (String value : new String[]{null, "", " ", "ftp://example.org", "https:///path",
                "https://example.org:65536", "https://example.org:0", "https://exa mple.org"}) {
            assertTrue(URIShorterUtil.getHttpUrl(value).isEmpty(), "Недопустимый URL: " + value);
        }
    }

    @Test
    void changingLimitPreservesCounterAndReactivatesLink() {
        UriWrapper uri = createUri();
        uri.setCallCount(2);
        uri.updateCallLimit(2);
        assertFalse(uri.isActive());
        uri.updateCallLimit(3);
        assertTrue(uri.isActive());
        assertEquals(2, uri.getCallCount());
        uri.updateCallLimit(0);
        assertFalse(uri.isActive());
        assertThrows(IllegalArgumentException.class, () -> uri.updateCallLimit(-1));
    }

    @Test
    void ttlExpiresExactlyAtDeadline() {
        UriWrapper uri = createUri();
        LocalDateTime deadline = uri.getActiveTo();
        assertFalse(uri.isExpired(deadline.minusNanos(1)));
        assertTrue(uri.isExpired(deadline));
        assertTrue(uri.isExpired(deadline.plusNanos(1)));
    }

    @Test
    void countsSuccessfulTransitionsAndBlocksAfterLimit() {
        UriWrapper uri = createUri();
        AtomicInteger calls = new AtomicInteger();
        ShortedUriExecuterService.UriOpener opener = target -> {
            assertEquals(uri.getUri(), target);
            calls.incrementAndGet();
        };
        ShortedUriExecuterService.executeRequest(uri, opener);
        ShortedUriExecuterService.executeRequest(uri, opener);
        ShortedUriExecuterService.executeRequest(uri, opener);
        assertEquals(2, calls.get());
        assertEquals(2, uri.getCallCount());
        assertFalse(uri.isActive());
    }

    @Test
    void browserFailureDoesNotConsumeTransition() {
        UriWrapper uri = createUri();
        ShortedUriExecuterService.executeRequest(uri, target -> {
            throw new IOException("Браузер недоступен");
        });
        assertEquals(0, uri.getCallCount());
        assertTrue(uri.isActive());
    }
}
