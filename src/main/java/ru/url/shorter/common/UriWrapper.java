package ru.url.shorter.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@AllArgsConstructor
public class UriWrapper {

    public static final String SHORTED_LINK_INFO_FORMAT = "ссылка  - %s, активна - %s, оставшиеся переходы  - %s, осталось секунд жизни  - %s";

    private URI uri;

    private int callCount;

    private int callLimit;

    private boolean active;

    private LocalDateTime activeTo;

    public String getInfo() {
        return SHORTED_LINK_INFO_FORMAT.formatted(
                uri,
                isActive(),
                callLimit - callCount,
                ChronoUnit.SECONDS.between(LocalDateTime.now(), activeTo)
        );
    }

    public boolean isExpired() {
        return isExpired(LocalDateTime.now());
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(activeTo);
    }

    public boolean isActive() {
        return active && !isExpired() && callCount < callLimit;
    }

    public void updateCallLimit(int callLimit) {
        if (callLimit < 0) {
            throw new IllegalArgumentException("Лимит не может быть отрицательным");
        }
        this.callLimit = callLimit;
        active = callCount < callLimit && !isExpired();
    }
}
