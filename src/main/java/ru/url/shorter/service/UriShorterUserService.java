package ru.url.shorter.service;

import ru.url.shorter.common.User;
import ru.url.shorter.exception.PasswordInvalidException;
import ru.url.shorter.exception.UserDuplicateException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UriShorterUserService {

    private static final Map<String, User> USER_MAP = new ConcurrentHashMap<>();

    public static final String ZERO_STRING = "0";

    public static final String WRITE_PASSWORD = "Введите пароль";

    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    public static final String USER_UUID = "UUID пользователя: %s";

    public static final String LOGIN_REGEX = "[A-Za-z0-9_.-]{1,64}";

    public static final String EXISTS_USERS = "Существующие логины пользователи:";

    public static final String CHOOSE_LOGIN = "Выберите логин из списка; 0 — отмена";

    public static final String WRITE_LOGIN = "Введите логин для регистрации; 0 — отмена";

    public static final String WRITE_NEW_PASSWORD = "Придумайте пароль для пользователя";

    public static final String PASSWORD_PROCESS_ERROR_MESSAGE = "Не удалось обработать пароль";

    public static final String INVALID_LOGIN_OR_PASSWORD_ERROR_MESSAGE = "Неверный логин или пароль";

    public static final String LOGIN_NOT_FOUND = "Нет пользователя с логином - %s. Попробуйте снова";

    public static final String USER_ALREADY_EXISTS_ERROR_MESSAGE = "Пользователь с логином %s уже существует";

    public static final String SECURITY_ERROR_MESSAGE = "Необходимо войти под зарегистрированным пользователем";

    public static final String NO_USER_REGISTER_NEW = "Нет зарегистрированных пользователей. Зарегистрируйте нового.";

    public static final String ILLEGAL_LOGIN_ERROR_MESSAGE = "Логин: 1–64 символа A–Z, a–z, 0–9, _, ., -; значение 0 занято отменой";

    public static final String ILLEGAL_PASSWORD_ERROR_MESSAGE = "Пароль должен содержать 1–1024 символа и не состоять только из пробелов";

    public static boolean containsShortedUri(String shortedUri) {
        return USER_MAP.values().stream()
                .map(User::getShortedUris)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .anyMatch(x -> x.equals(shortedUri));
    }

    public static User getUser(User user, Scanner scanner) {
        if (user != null) {
            return user;
        }
        if (USER_MAP.isEmpty()) {
            System.out.println(NO_USER_REGISTER_NEW);
            user = UriShorterUserService.registerUser(scanner);
        } else {
            user = UriShorterUserService.registerOrLogintUser(user, scanner);
        }
        return user;
    }

    public static User registerOrLogintUser(User user, Scanner scanner) {
        while (true) {
            System.out.println("""
                    Выберите действие:
                    0. Возврат в главное меню.
                    1. Залогиниться под существующем пользователем.
                    2. Зарегистрировать нового пользователя и переключиться на него.
                    """);
            int action = URIShorterUtil.scanLimitedInt(scanner, 0, 2);
            if (action == 0) {
                return user;
            }
            if (action == 1) {
                try {
                    User selectedUser = loginUser(scanner);
                    if (selectedUser != null) {
                        user = selectedUser;
                    }
                } catch (PasswordInvalidException | IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
            }
            if (action == 2) {
                try {
                    User selectedUser = registerUser(scanner);
                    if (selectedUser != null) {
                        user = selectedUser;
                    }
                } catch (UserDuplicateException | IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
            }
            return user;
        }
    }

    private static User registerUser(Scanner scanner) {
        System.out.println(WRITE_LOGIN);
        String login = scanner.nextLine().strip();
        if (login.equals(ZERO_STRING)) {
            return null;
        }
        System.out.println(WRITE_NEW_PASSWORD);
        String password = scanner.nextLine();
        return registerUser(login, password);
    }

    public static User registerUser(String login, String password) {
        validateCredentials(login, password);
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hashPassword(password, salt);
        byte[] passwordBytes = new byte[salt.length + hash.length];
        System.arraycopy(salt, 0, passwordBytes, 0, salt.length);
        System.arraycopy(hash, 0, passwordBytes, salt.length, hash.length);
        User user = new User(UUID.randomUUID(), login, passwordBytes, new ConcurrentHashMap<>());
        if (USER_MAP.putIfAbsent(login, user) != null) {
            throw new UserDuplicateException(USER_ALREADY_EXISTS_ERROR_MESSAGE.formatted(login));
        }
        System.out.println(USER_UUID.formatted(user.getUserId()));
        return user;
    }

    private static User loginUser(Scanner scanner) {
        String login = scanExistingUserLogin(scanner);
        if (login == null) {
            return null;
        }
        System.out.println(WRITE_PASSWORD);
        return loginUser(login, scanner.nextLine());
    }

    public static User loginUser(String login, String password) {
        validateCredentials(login, password);
        User user = USER_MAP.get(login);
        if (user == null) {
            throw new PasswordInvalidException(INVALID_LOGIN_OR_PASSWORD_ERROR_MESSAGE);
        }
        byte[] passwordBytes = user.getPassword();
        byte[] salt = Arrays.copyOf(passwordBytes, 16);
        byte[] expected = Arrays.copyOfRange(passwordBytes, 16, passwordBytes.length);
        if (!MessageDigest.isEqual(hashPassword(password, salt), expected)) {
            throw new PasswordInvalidException(INVALID_LOGIN_OR_PASSWORD_ERROR_MESSAGE);
        }
        return user;
    }

    private static void validateCredentials(String login, String password) {
        if (login == null || !login.matches(LOGIN_REGEX) || login.equals("0")) {
            throw new IllegalArgumentException(ILLEGAL_LOGIN_ERROR_MESSAGE);
        }
        if (password == null || password.isBlank() || password.length() > 1024) {
            throw new IllegalArgumentException(ILLEGAL_PASSWORD_ERROR_MESSAGE);
        }
    }

    private static byte[] hashPassword(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 600000, 256);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(PASSWORD_PROCESS_ERROR_MESSAGE, e);
        } finally {
            spec.clearPassword();
        }
    }

    private static void showUsers() {
        System.out.println(EXISTS_USERS);
        USER_MAP.keySet().forEach(System.out::println);
    }

    private static String scanExistingUserLogin(Scanner scanner) {
        System.out.println(CHOOSE_LOGIN);
        int counter = 0;
        while (true) {
            if (counter % 5 == 0) {
                showUsers();
            }
            String scanned = scanner.nextLine().strip();
            if (scanned.equals(ZERO_STRING)) {
                return null;
            }
            if (!isUserExist(scanned)) {
                System.out.println(LOGIN_NOT_FOUND.formatted(scanned));
                counter++;
                continue;
            }
            return scanned;
        }
    }

    public static boolean isUserExist(String login) {
        return login != null && USER_MAP.containsKey(login);
    }

    public static Optional<User> getUserByLogin(String login) {
        return login == null ? Optional.empty() : Optional.ofNullable(USER_MAP.get(login));
    }

    public static Optional<User> getUserById(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return USER_MAP.values().stream().filter(user -> userId.equals(user.getUserId())).findFirst();
    }

    static Collection<User> getUsers() {
        return List.copyOf(USER_MAP.values());
    }

    static void checkUser(User user) {
        if (user == null || getUserById(user.getUserId()).orElse(null) != user) {
            throw new SecurityException(SECURITY_ERROR_MESSAGE);
        }
    }
}
