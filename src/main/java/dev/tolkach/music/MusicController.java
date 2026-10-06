package dev.tolkach.music;

import dev.tolkach.yandex.model.PlaybackState;

public interface MusicController {

    PlaybackState playTrack(String title, String artist);

    PlaybackState playQuery(String query);

    PlaybackState playAlbum(String title, String artist);

    PlaybackState getPlaybackState();

    void pause();

    void resume();

    void next();

    void previous();
}
