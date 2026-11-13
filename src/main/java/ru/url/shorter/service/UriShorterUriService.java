package ru.url.shorter.service;

import ru.url.shorter.common.UriWrapper;
import ru.url.shorter.common.User;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;

public class UriShorterUriService {

    public static final int DEFAULT_SHORT_LENGTH = UriShorterConfig.getInstance().getShortLength();

    public static final int SHORTED_URI_SECONDS_LIFETIME = UriShorterConfig.getInstance().getTtlSeconds();

    public static final String ZERO = "0";

    public static final String PLUS = "+";

    public static final String MINUS = "-";

    public static final String URI_DELETED = "Ссылка удалена";

    public static final String URI_INFO = "Сокращение - %s, %s";

    public static final String URI_IS_CHANGE = "Ссылка изменена";

    public static final String LIMIT_IS_CHANGE = "Лимит изменён";

    public static final String ACTIVE_TO_CHANGED = "Время жизни изменено";

    public static final String NOT_REGISTERED_URI = "Нет зарегистрированных ссылок";

    public static final String WRITE_URI = "Введите новую HTTP/HTTPS-ссылку; 0 — отмена";

    public static final String IS_DELETE_URI = "Удалить ссылку? 0 — отмена; 1 — удалить";

    public static final String WRITE_NEW_URI = "Введите ссылку для регистрации; 0 — отмена";

    public static final String WRITE_SECONDS = "Введите количество секунд; 0 — без изменения";

    public static final String NEW_URI_REGISTERED = "Для ссылки %s зарегистрировано сокращение %s";

    public static final String NOT_FOUND_USER_URI_ERROR_MESSAGE = "Для пользователя %s доступны ссылки";

    public static final String INCORRECT_ACTIVE_TO_ERROR_MESSAGE = "Некорректное изменение времени жизни";

    public static final String DUPLICATE_URI_ERROR_MESSAGE = "Этот URI уже зарегистрирован у пользователя";

    public static final String SELECT_EXISTED_URI = "Выберите существующее сокращение ссылки; 0 — отмена: ";

    public static final String URI_NOT_FOUND_ERROR_MESSAGE = "Для пользователя %s нет зарегистрированных ссылок";

    public static final String WRITE_NEW_CALL_LIMIT = "Введите новый общий максимум обращений; 0 блокирует переходы";

    public static final String NO_ACTIVE_SHORTED_URI = "Нет сокращения ссылки с наименованием или она просрочилась - %s";

    public static final String UNIQ_SHORTING_ERROR_MESSAGE = "Не удалось создать уникальное сокращение. Увеличьте длину кода";

    public static final String WRITE_PLUS_MINUS = "Введите + для добавления времени или - для уменьшения времени жизни ссылки";

    public static final String USER_URI_NOT_FOUND_ERROR_MESSAGE = "Ссылка не найдена, удалена или принадлежит другому пользователю";

    public static final String AVALIABLE_URI_NOT_FOUND_ERROR_MESSAGE = "Ссылка не найдена, удалена или принадлежит другому пользователю";
    public static final String URI_EXPIRED = "Время жизни ссылки истекло";

    public static void uriWorker(User user, Scanner scanner) {
        while (true) {
            System.out.println("""
                    Доступные для пользователя %s действия со ссылками:
                    0 - Вернуться в главное меню.
                    1 - Зарегистрировать ссылку.
                    2 - Обратиться по сокращенной ссылке.
                    3 - Посмотреть зарегистрированные ссылки.
                    4 - Редактировать зарегистрированные ссылки.
                    5 - Удалить ссылку.
                    """.formatted(user.getLogin()));
            int action = ru.url.shorter.service.URIShorterUtil.scanLimitedInt(scanner, 0, 5);
            if (action == 0) {
                break;
            }
            try {
                if (action == 1) {
                    registerUri(user, scanner);
                }
                if (action == 2) {
                    executeUri(user, scanner);
                }
                if (action == 3) {
                    showRegisteredUri(user);
                }
                if (action == 4) {
                    manageRegisteredUri(user, scanner);
                }
                if (action == 5) {
                    deleteUri(user, scanner);
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void manageRegisteredUri(User user, Scanner scanner) {
        while (true) {
            System.out.println("""
                    Доступные для пользователя %s действия по изменению ссылок:
                    0 - Вернуться в меню действий со ссылками.
                    1 - Редактирование ссылки.
                    2 - Редактирование максимума обращений по ссылке.
                    3 - Редактирование времени жизни ссылки.
                    """.formatted(user.getLogin()));
            int action = ru.url.shorter.service.URIShorterUtil.scanLimitedInt(scanner, 0, 3);
            if (action == 0) {
                break;
            }
            if (action == 1) {
                refactorUri(user, scanner);
            }
            if (action == 2) {
                refactorCallLimit(user, scanner);
            }
            if (action == 3) {
                refactorActiveTo(user, scanner);
            }
        }
    }

    private static void refactorUri(User user, Scanner scanner) {
        String shortUri = getExistedShortUri(user.getShortedUris().keySet(), scanner);
        if (shortUri == null) {
            return;
        }
        System.out.println(WRITE_URI);
        URI uri = ru.url.shorter.service.URIShorterUtil.scanUri(scanner);
        if (uri != null) {
            refactorUri(user, shortUri, uri);
            System.out.println(URI_IS_CHANGE);
        }
    }

    static void refactorUri(User user, String shortUri, URI uri) {
        UriShorterUserService.checkUser(user);
        checkUri(uri);
        UriWrapper uriWrapper = getAvailableUri(user, shortUri);
        Optional<String> existing = getUri(uri, user);
        if (existing.isPresent() && !existing.get().equals(shortUri)) {
            throw new IllegalArgumentException(DUPLICATE_URI_ERROR_MESSAGE);
        }
        uriWrapper.setUri(uri);
    }

    private static void refactorCallLimit(User user, Scanner scanner) {
        String shortUri = getExistedShortUri(user.getShortedUris().keySet(), scanner);
        if (shortUri == null) {
            return;
        }
        System.out.println(WRITE_NEW_CALL_LIMIT);
        int callLimit = URIShorterUtil.scanLimitedInt(scanner, 0, Integer.MAX_VALUE);
        refactorCallLimit(user, shortUri, callLimit);
        System.out.println(LIMIT_IS_CHANGE);
    }

    static void refactorCallLimit(User user, String shortUri, int callLimit) {
        UriShorterUserService.checkUser(user);
        getAvailableUri(user, shortUri).updateCallLimit(callLimit);
    }

    private static void refactorActiveTo(User user, Scanner scanner) {
        String shortUri = getExistedShortUri(user.getShortedUris().keySet(), scanner);
        if (shortUri == null) {
            return;
        }
        System.out.println(WRITE_PLUS_MINUS);
        String operation = URIShorterUtil.scanPlusMinus(scanner);
        System.out.println(WRITE_SECONDS);
        int seconds = URIShorterUtil.scanLimitedInt(scanner, 0, 315360000);
        refactorActiveTo(user, shortUri, operation, seconds);
        System.out.println(ACTIVE_TO_CHANGED);
    }

    static void refactorActiveTo(User user, String shortUri, String operation, int seconds) {
        UriShorterUserService.checkUser(user);
        if ((!operation.equals(PLUS) && !operation.equals(MINUS)) || seconds < 0 || seconds > 315360000) {
            throw new IllegalArgumentException(INCORRECT_ACTIVE_TO_ERROR_MESSAGE);
        }
        UriWrapper uriWrapper = getAvailableUri(user, shortUri);
        LocalDateTime activeTo = uriWrapper.getActiveTo();
        if (operation.equals(PLUS)) {
            activeTo = activeTo.plusSeconds(seconds);
        } else {
            activeTo = activeTo.minusSeconds(seconds);
        }
        uriWrapper.setActiveTo(activeTo);
    }

    private static void showRegisteredUri(User user) {
        UriShorterUserService.checkUser(user);
        Map<String, UriWrapper> shortedUris = user.getShortedUris();
        if (shortedUris.isEmpty()) {
            System.out.println(URI_NOT_FOUND_ERROR_MESSAGE.formatted(user.getLogin()));
        } else {
            System.out.println(NOT_FOUND_USER_URI_ERROR_MESSAGE.formatted(user.getLogin()));
            shortedUris.forEach((shortUri, uriWrapper) ->
                    System.out.println(URI_INFO.formatted(shortUri, uriWrapper.getInfo())));
        }
    }

    private static void registerUri(User user, Scanner scanner) {
        System.out.println(WRITE_NEW_URI);
        URI uri = URIShorterUtil.scanUri(scanner);
        if (uri == null) {
            return;
        }
        String shortedUri = registerUri(user, uri);
        System.out.println(NEW_URI_REGISTERED.formatted(uri, shortedUri));
    }

    static String registerUri(User user, URI uri) {
        UriShorterUserService.checkUser(user);
        checkUri(uri);
        Optional<String> optionalShortedUri = getUri(uri, user);
        if (optionalShortedUri.isPresent()) {
            return optionalShortedUri.get();
        }
        for (int i = 0; i < UriShorterConfig.getInstance().getGenerationAttempts(); i++) {
            String shortedUri = URIShorterUtil.shortURI(DEFAULT_SHORT_LENGTH);
            if (!UriShorterUserService.containsShortedUri(shortedUri)) {
                UriWrapper uriWrapper = new UriWrapper(uri, 0, UriShorterConfig.getInstance().getCallLimit(),
                        true, LocalDateTime.now().plusSeconds(SHORTED_URI_SECONDS_LIFETIME));
                user.getShortedUris().put(shortedUri, uriWrapper);
                return shortedUri;
            }
        }
        throw new IllegalStateException(UNIQ_SHORTING_ERROR_MESSAGE);

    }

    private static void executeUri(User user, Scanner scanner) {
        String shortUri = getExistedShortUri(user.getShortedUris().keySet(), scanner);
        if (shortUri == null) {
            return;
        }
        executeUri(user, shortUri, ShortedUriExecuterService::openInBrowser);
    }

    static void executeUri(User user, String shortUri, ru.url.shorter.service.ShortedUriExecuterService.UriOpener opener) {
        UriShorterUserService.checkUser(user);
        ShortedUriExecuterService.executeRequest(getAvailableUri(user, shortUri), opener);
    }

    private static void deleteUri(User user, Scanner scanner) {
        String shortUri = getExistedShortUri(user.getShortedUris().keySet(), scanner);
        if (shortUri == null) {
            return;
        }
        System.out.println(IS_DELETE_URI);
        if (URIShorterUtil.scanLimitedInt(scanner, 0, 1) == 1) {
            deleteUri(user, shortUri);
            System.out.println(URI_DELETED);
        }
    }

    static void deleteUri(User user, String shortUri) {
        UriShorterUserService.checkUser(user);
        if (user.getShortedUris().remove(shortUri) == null) {
            throw new IllegalStateException(USER_URI_NOT_FOUND_ERROR_MESSAGE);
        }

    }

    static String getExistedShortUri(Set<String> shorterUriSet, Scanner scanner) {
        int counter = 0;
        while (true) {
            if (shorterUriSet.isEmpty()) {
                System.out.println(NOT_REGISTERED_URI);
                return null;
            }
            if (counter % 5 == 0) {
                System.out.println(SELECT_EXISTED_URI);
                shorterUriSet.forEach(System.out::println);
            }
            String scanned = scanner.nextLine().strip();
            if (scanned.equals(ZERO)) {
                return null;
            }
            if (!shorterUriSet.contains(scanned)) {
                System.out.println(NO_ACTIVE_SHORTED_URI.formatted(scanned));
                counter++;
                continue;
            }
            return scanned;
        }
    }

    private static Optional<String> getUri(URI uri, User user) {
        for (Map.Entry<String, UriWrapper> entry : user.getShortedUris().entrySet()) {
            if (!entry.getValue().isExpired() && entry.getValue().getUri().equals(uri)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private static UriWrapper getAvailableUri(User user, String shortUri) {
        UriWrapper uriWrapper = user.getShortedUris().get(shortUri);
        if (uriWrapper == null) {
            throw new IllegalStateException(AVALIABLE_URI_NOT_FOUND_ERROR_MESSAGE);
        }
        if (uriWrapper.isExpired()) {
            throw new IllegalStateException(URI_EXPIRED);
        }
        return uriWrapper;
    }

    private static void checkUri(URI uri) {
        if (uri == null || URIShorterUtil.getHttpUrl(uri.toString()).isEmpty()) {
            throw new IllegalArgumentException(URIShorterUtil.MUST_BE_HTTP);
        }
    }
}
