package ru.url.shorter.service;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
public class UriShorterConfig {

    private final String host;

    private final int callLimit;

    private final int ttlSeconds;

    private final int shortLength;

    private final int cleanupSeconds;

    private final int generationAttempts;

    public static final String TTL_PROPERTY = "shorter.default-ttl-seconds";

    public static final String SHORT_LENGTH_PROPERTY = "shorter.code-length";

    public static final String SHORTER_BASE_URL_PROPERTY = "shorter.base-url";

    public static final String CALL_LIMIT_PROPERTY = "shorter.default-call-limit";

    public static final String GENERATION_ATTEMPTS_PROPERTY = "shorter.generation-attempts";

    public static final String CLEANUP_INTERVAL_SECONDS_PROPERTY = "shorter.cleanup-interval-seconds";

    public static final String EMPTY_STRING = "";

    public static final String SLASH_REGEX = "/+$";

    public static final String APPLICATION_PROPERTIES_PATH = "/application.properties";

    public static final String MUST_BE_INTEGER = "Параметр %s должен быть целым числом";

    public static final String APPLICATION_PROPERTIES_NOT_FOUND = "Не найден application.properties";

    public static final String MUST_BE_BETWEEN_ERROR_MESSAGE = "Параметр %s должен быть от %s до %s";

    public static final String CONFIG_NO_READABLE_ERROR_MESSAGE = "Не удалось прочитать конфигурацию: ";

    public static final String HOST_FORMAT_ERROR_MESSAGE = "Префикс сокращения не должен быть пустым или содержать пробелы";

    private UriShorterConfig(Properties properties) {
        ttlSeconds = readInt(properties, TTL_PROPERTY, 1, 315360000);
        callLimit = readInt(properties, CALL_LIMIT_PROPERTY, 0, Integer.MAX_VALUE);
        shortLength = readInt(properties, SHORT_LENGTH_PROPERTY, 1, 64);
        cleanupSeconds = readInt(properties, CLEANUP_INTERVAL_SECONDS_PROPERTY, 1, 86400);
        generationAttempts = readInt(properties, GENERATION_ATTEMPTS_PROPERTY, 1, 100000);
        host = properties.getProperty(SHORTER_BASE_URL_PROPERTY, EMPTY_STRING).strip().replaceAll(SLASH_REGEX, EMPTY_STRING);
        if (host.isBlank() || host.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(HOST_FORMAT_ERROR_MESSAGE);
        }
    }

    private static class Holder {
        private static final UriShorterConfig INSTANCE = load();
    }

    public static UriShorterConfig getInstance() {
        return Holder.INSTANCE;
    }

    public static UriShorterConfig load() {
        Properties properties = new Properties();
        try (InputStream stream = UriShorterConfig.class.getResourceAsStream(APPLICATION_PROPERTIES_PATH)) {
            if (stream == null) {
                throw new IllegalArgumentException(APPLICATION_PROPERTIES_NOT_FOUND);
            }
            properties.load(stream);
            return new UriShorterConfig(properties);
        } catch (IOException e) {
            throw new IllegalArgumentException(CONFIG_NO_READABLE_ERROR_MESSAGE + e.getMessage(), e);
        }
    }

    private static int readInt(Properties properties, String name, int min, int max) {
        try {
            int value = Integer.parseInt(properties.getProperty(name));
            if (value < min || value > max) {
                throw new IllegalArgumentException(MUST_BE_BETWEEN_ERROR_MESSAGE.formatted(name, min, max));
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MUST_BE_INTEGER.formatted(name), e);
        }
    }
}
