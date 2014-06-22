package me.rkfg.ns2gather.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.ppsrk.gwt.client.AlertRuntimeException;

import com.google.gwt.dom.client.MediaElement;
import com.google.gwt.media.client.Audio;

public class SoundManager {
    String[] soundURLs = { "chat.ogg", "vote.ogg", "enter.ogg", "leave.ogg", "timer.ogg", "more.ogg" };
    List<Audio> audios = new ArrayList<>();
    Set<NS2Sound> soundsToPlay = new HashSet<NS2Sound>();

    public SoundManager() {
        for (String soundURL : soundURLs) {
            Audio newAudio = Audio.createIfSupported();
            if (newAudio == null) {
                throw new AlertRuntimeException("Браузер не поддерживает звуки (HTML5 элемент <audio>)");
            }
            newAudio.setPreload(MediaElement.PRELOAD_AUTO);
            newAudio.setSrc("audio/" + soundURL);
            newAudio.setAutoplay(false);
            audios.add(newAudio);
        }
    }

    public void playSound(NS2Sound sound) {
        Audio audio = getSound(sound);
        if (!audio.isPaused()) {
            stopSound(sound);
        }
        audio.play();
    }

    public void stopSound(NS2Sound sound) {
        Audio audio = getSound(sound);
        audio.pause();
        audio.setCurrentTime(0);
    }

    private Audio getSound(NS2Sound sound) {
        if (sound.ordinal() >= audios.size()) {
            throw new AlertRuntimeException("no such sound");
        }
        return audios.get(sound.ordinal());
    }

    public void setVolume(NS2Sound sound, double volume) {
        Audio audio = getSound(sound);
        audio.setVolume(volume);
    }

    public void setLoop(NS2Sound sound, boolean loop) {
        Audio audio = getSound(sound);
        audio.setLoop(loop);
    }

    public void queue(NS2Sound sound) {
        soundsToPlay.add(sound);
    }

    public void playQueued() {
        for (NS2Sound sound : soundsToPlay) {
            playSound(sound);
        }
        clearQueue();
    }

    public void clearQueue() {
        soundsToPlay.clear();
    }
}
