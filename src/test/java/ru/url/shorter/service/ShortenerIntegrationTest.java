package ru.url.shorter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.url.shorter.common.UriWrapper;
import ru.url.shorter.common.User;
import ru.url.shorter.exception.PasswordInvalidException;
import ru.url.shorter.exception.UserDuplicateException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ShortenerIntegrationTest {

    private User user;

    @BeforeEach
    void registerUser() {
        user = UriShorterUserService.registerUser(UUID.randomUUID().toString(), "secret");
    }

    private String registerUri() {
        return UriShorterUriService.registerUri(user, URI.create("https://example.org"));
    }

    @Test
    void registersAndAuthenticatesUser() {
        assertNotNull(user.getUserId());
        assertSame(user, UriShorterUserService.loginUser(user.getLogin(), "secret"));
        assertSame(user, UriShorterUserService.getUserById(user.getUserId()).orElseThrow());
        assertThrows(PasswordInvalidException.class,
                () -> UriShorterUserService.loginUser(user.getLogin(), "wrong"));
        assertThrows(UserDuplicateException.class,
                () -> UriShorterUserService.registerUser(user.getLogin(), "another"));
    }

    @Test
    void keepsSeparateLinksForUsersAndReusesDuplicateUrl() {
        String first = registerUri();
        assertEquals(first, registerUri());
        String second = UriShorterUriService.registerUri(user, URI.create("https://second.org"));
        User other = UriShorterUserService.registerUser(UUID.randomUUID().toString(), "secret");
        String third = UriShorterUriService.registerUri(other, URI.create("https://example.org"));
        assertEquals(2, user.getShortedUris().size());
        assertEquals(1, other.getShortedUris().size());
        assertNotEquals(first, second);
        assertNotEquals(first, third);
        assertNotEquals(second, third);
    }

    @Test
    void editingUrlPreservesUsageAndRejectsDuplicate() {
        String shortUri = registerUri();
        UriShorterUriService.refactorCallLimit(user, shortUri, 3);
        UriShorterUriService.executeUri(user, shortUri, target -> {});
        LocalDateTime deadline = user.getShortedUris().get(shortUri).getActiveTo();
        UriShorterUriService.refactorUri(user, shortUri, URI.create("https://changed.org"));
        UriWrapper uri = user.getShortedUris().get(shortUri);
        assertEquals(URI.create("https://changed.org"), uri.getUri());
        assertEquals(1, uri.getCallCount());
        assertEquals(deadline, uri.getActiveTo());
        UriShorterUriService.registerUri(user, URI.create("https://existing.org"));
        assertThrows(IllegalArgumentException.class, () -> UriShorterUriService.refactorUri(
                user, shortUri, URI.create("https://existing.org")));
    }

    @Test
    void rejectsAnotherOwnerAndForgedUser() {
        String shortUri = registerUri();
        User other = UriShorterUserService.registerUser(UUID.randomUUID().toString(), "secret");
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.refactorUri(other, shortUri, URI.create("https://other.org")));
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.refactorCallLimit(other, shortUri, 10));
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.refactorActiveTo(other, shortUri, "+", 60));
        assertThrows(IllegalStateException.class, () -> UriShorterUriService.deleteUri(other, shortUri));
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.executeUri(other, shortUri, target -> fail("Чужая ссылка открыта")));
        User fake = new User(user.getUserId(), user.getLogin(), user.getPassword(), new ConcurrentHashMap<>());
        assertThrows(SecurityException.class, () -> UriShorterUriService.deleteUri(fake, shortUri));
        assertTrue(user.getShortedUris().containsKey(shortUri));
    }

    @Test
    void updatedLimitControlsNextTransitions() {
        String shortUri = registerUri();
        AtomicInteger calls = new AtomicInteger();
        UriShorterUriService.refactorCallLimit(user, shortUri, 0);
        UriShorterUriService.executeUri(user, shortUri, target -> calls.incrementAndGet());
        assertEquals(0, calls.get());
        UriShorterUriService.refactorCallLimit(user, shortUri, 1);
        UriShorterUriService.executeUri(user, shortUri, target -> calls.incrementAndGet());
        UriShorterUriService.executeUri(user, shortUri, target -> calls.incrementAndGet());
        assertEquals(1, calls.get());
        assertEquals(1, user.getShortedUris().get(shortUri).getCallCount());
    }

    @Test
    void ttlCanBeAdjustedButExpiredLinkCannotBeOpenedOrExtended() {
        String shortUri = registerUri();
        UriWrapper uri = user.getShortedUris().get(shortUri);
        LocalDateTime deadline = uri.getActiveTo();
        UriShorterUriService.refactorActiveTo(user, shortUri, "+", 60);
        assertEquals(deadline.plusSeconds(60), uri.getActiveTo());
        UriShorterUriService.refactorActiveTo(user, shortUri, "-", 30);
        assertEquals(deadline.plusSeconds(30), uri.getActiveTo());
        uri.setActiveTo(LocalDateTime.now().minusMinutes(1));
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.executeUri(user, shortUri, target -> fail("Просроченная ссылка открыта")));
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.refactorActiveTo(user, shortUri, "+", 60));
    }

    @Test
    void cleanupRemovesExpiredLinkAndKeepsLiveLink() {
        String expired = registerUri();
        user.getShortedUris().get(expired).setActiveTo(LocalDateTime.now().minusMinutes(1));
        String live = UriShorterUriService.registerUri(user, URI.create("https://live.org"));
        user.getShortedUris().get(live).setActiveTo(LocalDateTime.now().plusHours(1));
        ShortedUriObserver.deleteOldUri();
        assertFalse(user.getShortedUris().containsKey(expired));
        assertTrue(user.getShortedUris().containsKey(live));
    }

    @Test
    void deletedLinkIsUnavailableForFurtherOperations() {
        String shortUri = registerUri();
        UriShorterUriService.deleteUri(user, shortUri);
        assertFalse(user.getShortedUris().containsKey(shortUri));
        assertThrows(IllegalStateException.class, () -> UriShorterUriService.refactorCallLimit(user, shortUri, 10));
        assertThrows(IllegalStateException.class,
                () -> UriShorterUriService.executeUri(user, shortUri, target -> fail("Удалённая ссылка открыта")));
    }

    @Test
    void registrationUsesPropertiesConfiguration() {
        UriShorterConfig config = UriShorterConfig.load();
        LocalDateTime before = LocalDateTime.now();
        String shortUri = registerUri();
        LocalDateTime after = LocalDateTime.now();
        UriWrapper uri = user.getShortedUris().get(shortUri);
        assertEquals(config.getCallLimit(), uri.getCallLimit());
        String prefix = config.getHost() + "/";
        assertTrue(shortUri.startsWith(prefix));
        assertEquals(config.getShortLength(), shortUri.substring(prefix.length()).length());
        assertFalse(uri.getActiveTo().isBefore(before.plusSeconds(config.getTtlSeconds())));
        assertFalse(uri.getActiveTo().isAfter(after.plusSeconds(config.getTtlSeconds())));
    }

    @Test
    void consoleCanLoginListLinksAndCancelUserSwitch() {
        String shortUri = registerUri();
        String input = "1\n" + user.getLogin() + "\nsecret\n2\n3\n0\n1\n0\n0\n";
        InputStream previousInput = System.in;
        PrintStream previousOutput = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(capture);
            UriShorterMainMenu.main(new String[0]);
        } finally {
            System.setIn(previousInput);
            System.setOut(previousOutput);
        }
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains(shortUri));
        assertEquals(3, text.split("Для пользователя " + user.getLogin() + " доступны действия:", -1).length - 1);
        assertFalse(text.contains("Нет активного пользователя"));
    }
}
