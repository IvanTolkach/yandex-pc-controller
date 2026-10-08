package dev.tolkach.music.launcher;

import java.time.Duration;

public record YandexMusicLaunchConfig(String cdpUrl, Duration startupTimeout, Duration probeInterval) {

    public YandexMusicLaunchConfig {
        if (cdpUrl == null || cdpUrl.isBlank()) {
            throw new IllegalArgumentException("cdpUrl must not be empty");
        }

        if (startupTimeout == null || startupTimeout.isNegative()) {
            throw new IllegalArgumentException("startupTime must not be empty");
        }

        if (probeInterval == null || probeInterval.isNegative()) {
            throw new IllegalArgumentException("probeInterval must not be empty");
        }
    }
}
