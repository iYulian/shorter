package ru.url.shorter.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

public class URIShorterUtil {

    public static final String SUMBOLS_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static final String SHORT_URI_FORMAT = "%s/%s";

    public static final String HTTP = "http";

    public static final String HTTPS = "https";

    public static final String HOST_ROF_SHORTED = UriShorterConfig.getInstance().getHost();

    public static final String SCAN_INT_EX = "Необходимо выбрать числовое значение от %s до %s включительно";

    public static final String SCAN_PLUS_MINUS_EX = "Необходимо выбрать либо '+' либо '-'";

    public static final String MUST_BE_HTTP = "Строка должна иметь формат HTTP/HTTPS-ссылки, например https://example.org/page?q=1.";

    public static final String LENGTH_MUST_BE_MOR_THAN_ONE = "Длина сокращения должна быть больше 1";


    public static String scanPlusMinus(Scanner scanner) {
        while (true) {
            String scanned = scanner.nextLine().strip();
            if (!scanned.equals("+") && !scanned.equals("-")) {
                System.out.println(SCAN_PLUS_MINUS_EX);
                continue;
            }
            return scanned;
        }
    }

    public static int scanLimitedInt(Scanner scanner, int minInt, int maxInt) {
        while (true) {
            try {
                String scanned = scanner.nextLine().strip();
                int scannedInt = Integer.parseInt(scanned);
                if (scannedInt < minInt || scannedInt > maxInt) {
                    System.out.println(SCAN_INT_EX.formatted(minInt, maxInt));
                    continue;
                }
                return scannedInt;
            } catch (NumberFormatException e) {
                System.out.println(SCAN_INT_EX.formatted(minInt, maxInt));
            }
        }
    }

    public static URI scanUri(Scanner scanner) {
        while (true) {
            String url = scanner.nextLine().strip();
            if (url.equals("0")) {
                return null;
            }
            Optional<URI> uri = URIShorterUtil.getHttpUrl(url);
            if (uri.isEmpty()) {
                System.out.println(MUST_BE_HTTP);
                continue;
            }
            return uri.get();
        }
    }

    public static Optional<URI> getHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(value);
            if ((HTTP.equalsIgnoreCase(uri.getScheme()) || HTTPS.equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && (uri.getPort() == -1 || uri.getPort() >= 1 && uri.getPort() <= 65535)) {
                return Optional.of(uri);
            }
            return Optional.empty();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    public static String shortURI(int shortLength) {
        String generatedRandomString = generateRandomString(shortLength);
        return SHORT_URI_FORMAT.formatted(HOST_ROF_SHORTED, generatedRandomString);
    }

    private static String generateRandomString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException(LENGTH_MUST_BE_MOR_THAN_ONE);
        }
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(SUMBOLS_STRING.charAt(random.nextInt(SUMBOLS_STRING.length())));
        }
        return sb.toString();
    }
}
