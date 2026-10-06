package dev.tolkach.yandex.matching;

import dev.tolkach.music.matching.TextNormalizer;
import dev.tolkach.yandex.model.TrackSearchResult;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TrackMatcher {

    private final TextNormalizer normalizer = new TextNormalizer();

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

    private double similarity(String first, String second) {
        String a = normalizer.compact(first);

        String b = normalizer.compact(second);

        if (a.isEmpty() || b. isEmpty()) {
            return 0.0;
        }

        if (a.length() < 3 || b.length() < 3) {
            return a.equals(b) ? 1.0 : 0.0;
        }

        int distance = levenshteinDistance(a, b);

        int maxLength = Math.max(a.length(), b.length());

        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String first, String second) {
        int firstLength = first.length();
        int secondLength = second.length();

        int[][] distance = new int[firstLength + 1][secondLength + 1];

        for (int i = 0; i <= firstLength; i++) {
            distance[i][0] = i;
        }

        for (int j = 0; j <= secondLength; j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= firstLength; i++) {
            for (int j = 1; j <= secondLength; j++) {
                int substitutionCost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;

                distance[i][j] = Math.min(Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + substitutionCost
                );
            }
        }

        return distance[firstLength][secondLength];
    }

    private double artistSimilarity(TrackSearchResult result, String requestedArtist) {
        if (requestedArtist == null || requestedArtist.isBlank()) {
            return 0.0;
        }

        return result.artists()
                .stream()
                .mapToDouble(artist -> similarity(artist, requestedArtist))
                .max()
                .orElse(0.0);
    }

    private record ScoredResult(TrackSearchResult result, int score) {

    }
}

