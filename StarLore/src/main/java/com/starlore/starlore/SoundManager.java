package com.starlore.starlore;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundManager {

    private static MediaPlayer menuMusicPlayer;

    public static void playMenuMusic() {
        if (menuMusicPlayer == null) {
            Media media = new Media(SoundManager.class.getResource("sounds/menu_theme.wav").toExternalForm());
            menuMusicPlayer = new MediaPlayer(media);
            menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            menuMusicPlayer.setVolume(0.4);
        }
        if (menuMusicPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            menuMusicPlayer.play();
        }
    }

    public static void stopMenuMusic() {
        if (menuMusicPlayer != null) {
            menuMusicPlayer.stop();
        }
    }
}
