package dev.tolkach.yandex.matching;

import dev.tolkach.yandex.model.TrackSearchResult;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TrackMatcher {

    public Optional<TrackSearchResult> findBestMatch(List<TrackSearchResult> results, String requestedTitle, String requestedArtist) {
        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        String title = normalize(requestedTitle);
        String artist = normalize(requestedArtist);

        return results.stream()
                .map(result -> new ScoredResult(result, calculateScore(result, title, artist)))
                .filter(result -> result.score() > 0)
                .max(Comparator.comparingInt(ScoredResult::score))
                .map(ScoredResult::result);
    }

    private int calculateScore(TrackSearchResult result, String requestedTitle, String requestedArtist) {
        String actualTitle = normalize(result.title());
        List<String> actualArtists = result.artists().stream().map(this::normalize).toList();

        boolean titleExact = actualTitle.equals(requestedTitle);
        boolean artistExact = actualArtists.stream().anyMatch(requestedArtist::equals);

        boolean titleContains = actualTitle.contains(requestedTitle);
        boolean artistContains = actualArtists.stream().anyMatch(artist -> artist.contains(requestedArtist));

        if (titleExact && artistExact) {
            return 1000;
        }
        if (titleExact && artistContains) {
            return 800;
        }
        if (titleContains && artistExact) {
            return 700;
        }
        if (titleContains && artistContains) {
            return 500;
        }

        return 0;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace("ё","е")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ScoredResult(TrackSearchResult result, int score) {

    }
}

