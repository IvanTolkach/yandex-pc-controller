package dev.tolkach.music;

import dev.tolkach.yandex.YandexMusicClient;
import dev.tolkach.yandex.model.PlaybackState;

public class YandexMusicController implements MusicController {

    private final YandexMusicClient client;

    public YandexMusicController(YandexMusicClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Client must not be null");
        }

        this.client = client;
    }

    @Override
    public PlaybackState playTrack(String title, String artist) {
        return client.playTrack(title, artist);
    }

    @Override
    public PlaybackState playQuery(String query) {
        return client.playQuery(query);
    }

    @Override
    public PlaybackState playAlbum(String title, String artist) {
        return client.playAlbum(title, artist);
    }

    @Override
    public PlaybackState getPlaybackState() {
        return client.getPlaybackState();
    }

    @Override
    public void pause() {
        client.pause();
    }

    @Override
    public void resume() {
        client.resume();
    }

    @Override
    public void next() {
        client.next();
    }

    @Override
    public void previous() {
        client.previous();
    }
}
