package ru.url.shorter.service;

import ru.url.shorter.common.UriWrapper;
import ru.url.shorter.common.User;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ShortedUriObserver implements AutoCloseable {

    public static final String URI_EXPIRED = "!!!Время жизни истекло: удалено сокращение %s пользователя %s (UUID %s)!!!";

    public static final String DELETE_OLD_URI_ERROR_MESSAGE = "Ошибка очистки устаревших ссылок пользователя. Ошибка - %s";

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public ShortedUriObserver() {
        int interval = UriShorterConfig.getInstance().getCleanupSeconds();
        executorService.scheduleWithFixedDelay(ShortedUriObserver::deleteOldUri, interval, interval, TimeUnit.SECONDS);
    }

    static void deleteOldUri() {
        try {
            for (User user : UriShorterUserService.getUsers()) {
                user.getShortedUris().entrySet().removeIf(x -> {
                    UriWrapper uriWrapper = x.getValue();
                    if (uriWrapper.isExpired()) {
                        System.out.println(URI_EXPIRED.formatted(x.getKey(), user.getLogin(), user.getUserId()));
                        return true;
                    }
                    return false;
                });
            }
        } catch (Exception e) {
            System.out.println(DELETE_OLD_URI_ERROR_MESSAGE.formatted(e.getMessage()));
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
    }
}
