package ru.url.shorter.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserDuplicateException extends RuntimeException {
    public UserDuplicateException(String message) {
        super(message);
    }
}
