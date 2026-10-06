package dev.tolkach.music.matching;

import java.text.Normalizer;
import java.util.Locale;

public class TextNormalizer {

    public String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String compact(String value) {
        return normalize(value).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
