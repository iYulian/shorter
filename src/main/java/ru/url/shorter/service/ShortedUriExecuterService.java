package ru.url.shorter.service;

import ru.url.shorter.common.UriWrapper;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class ShortedUriExecuterService {

    public static final String URI_NOT_ACTIVE = "Ссылка не активна";

    public static final String URI_EXPIRED = "Время жизни ссылки истекло";

    public static final String URI_NOT_FOUND = "Ссылка не найдена или уже удалена";

    public static final String CALL_LIMIT_EXCESS = "Превышен лимит обращений по ссылке";

    public static final String BROWSER_UNAVALIABLE = "Открытие браузера недоступно в этой среде";

    public static final String LAST_URI_CALL = "!!!Последнее обращение по сокращенной ссылке. Ссылка становится не активной!!!";

    public static final String URI_EXECUTION_EXCEPTION = "Ошибка при попытке перейти по ссылке в браузер. Попытка обращения не засчитана. Ошибка - %s";

    static void executeRequest(UriWrapper uriWrapper, UriOpener opener) {
        if (uriWrapper == null) {
            System.out.println(URI_NOT_FOUND);
            return;
        }
            if (uriWrapper.isExpired()) {
                System.out.println(URI_EXPIRED);
                return;
            }
            if (uriWrapper.getCallCount() >= uriWrapper.getCallLimit()) {
                System.out.println(CALL_LIMIT_EXCESS);
                uriWrapper.setActive(false);
                return;
            }
            if (!uriWrapper.isActive()) {
                System.out.println(URI_NOT_ACTIVE);
                return;
            }
            try {
                opener.open(uriWrapper.getUri());
                uriWrapper.setCallCount(uriWrapper.getCallCount() + 1);
            } catch (Exception e) {
                System.out.println(URI_EXECUTION_EXCEPTION.formatted(e.getMessage()));
                return;
            }
            if (uriWrapper.getCallCount() >= uriWrapper.getCallLimit()) {
                System.out.println(LAST_URI_CALL);
                uriWrapper.setActive(false);
            }

    }

    static void openInBrowser(URI uri) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException(BROWSER_UNAVALIABLE);
        }
        Desktop.getDesktop().browse(uri);
    }

    @FunctionalInterface
    interface UriOpener {
        void open(URI uri) throws Exception;
    }
}
