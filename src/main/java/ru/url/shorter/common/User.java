package ru.url.shorter.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class User {

    private final UUID userId;

    private final String login;

    private final byte[] password;

    private final Map<String, UriWrapper> shortedUris;
}
