package dev.tolkach.yandex.matching;

import dev.tolkach.music.matching.TextNormalizer;
import dev.tolkach.yandex.model.AlbumSearchResult;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AlbumMatcher {

    private final TextNormalizer normalizer;

    public AlbumMatcher(TextNormalizer normalizer) {
        if (normalizer == null) {
            throw  new IllegalArgumentException("normalizer must not be null");
        }

        this.normalizer = normalizer;
    }

    public Optional<AlbumSearchResult> findBestMatch(List<AlbumSearchResult> results, String requestedTitle) {
        if (results == null || results.isEmpty()) {
            return  Optional.empty();
        }

        String normalizedRequested = normalizer.normalize(requestedTitle);

        return results
                .stream()
                .map(result -> new ScoredResult(result, calculateScore(result, normalizedRequested)))
                .filter(scored -> scored.score() > 0)
                .max(Comparator.comparingInt(ScoredResult::score))
                .map(ScoredResult::result);
    }

    private int calculateScore(AlbumSearchResult result, String requestedTitle) {
        String actualTitle = normalizer.normalize(result.title());

        if (actualTitle.equals(requestedTitle)) {
            return 1000;
        }

        if (normalizer.compact(actualTitle).equals(normalizer.compact(requestedTitle))) {
            return 900;
        }

        if (actualTitle.contains(requestedTitle)) {
            return 700;
        }

        return 0;
    }

    private record ScoredResult(AlbumSearchResult result, int score) {

    }
}
