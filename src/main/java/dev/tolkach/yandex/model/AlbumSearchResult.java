package dev.tolkach.yandex.model;

public record AlbumSearchResult(String title, String albumId) {

    public AlbumSearchResult {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Album title must not be empty");
        }

        if (albumId == null || albumId.isBlank()) {
            throw new IllegalArgumentException("Album id must not be empty");
        }

        title = title.trim();
        albumId = albumId.trim();
    }

    @Override
    public String toString() {
        return title + " [albumId=" + albumId + "]";
    }
}
