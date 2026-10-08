package dev.tolkach.music.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class YandexMusicExecutableLocator {

    private static final String SYSTEM_PROPERTY = "yandex.music.executable";

    private static final String ENVIRONMENT_VARIABLE = "YANDEX_MUSIC_EXE";

    public Path locate() {
        List<Path> candidates = new ArrayList<>();

        String property = System.getProperty(SYSTEM_PROPERTY);

        if (property != null && !property.isBlank()) {
            candidates.add(Path.of(property));
        }

        String environment = System.getenv(ENVIRONMENT_VARIABLE);

        if (environment != null && !environment.isBlank()) {
            candidates.add(Path.of(environment));
        }

        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            Path yandexMusicDirectory = Path.of(localAppData, "Programs", "YandexMusic");

            candidates.add(yandexMusicDirectory.resolve("Яндекс Музыка.exe"));
            candidates.add(yandexMusicDirectory.resolve("Yandex Music.exe"));
            candidates.add(yandexMusicDirectory.resolve("YandexMusic.exe"));
        }

        return candidates.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        """
                               Yandex Music executable was not found.
                               Configure it with:
                               -D%s=<path>
                               or environment variable %s
                               """.formatted(
                                SYSTEM_PROPERTY,
                                ENVIRONMENT_VARIABLE

                        )
                ));
    }

}
