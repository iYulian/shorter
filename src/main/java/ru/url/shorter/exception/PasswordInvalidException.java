package ru.url.shorter.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PasswordInvalidException extends RuntimeException {
    public PasswordInvalidException(String message) {
        super(message);
    }
}
