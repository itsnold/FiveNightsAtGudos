package com.gudos.audio;

import com.gudos.assets.AssetManager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Audio decoding and mixer calls never block Swing. Missing hardware degrades to silence. */
public final class AudioService implements AutoCloseable {
    // Facade: callers request sounds through play/ambience/menuMusic instead of managing Java Sound.
    // One worker owns clip operations; its daemon thread does not keep the JVM alive on its own.
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "gudo-audio");
        thread.setDaemon(true);
        return thread;
    });
    // Load each sound lazily and reuse its clip. Failed sounds are remembered to avoid repeated errors.
    private final Map<Sound, Clip> clips = new EnumMap<>(Sound.class);
    private final Set<Sound> unavailable = EnumSet.noneOf(Sound.class);
    private volatile int volume;

    public AudioService(int volume) { this.volume = volume; }

    // Find an already-open clip, or decode its bundled resource the first time it is requested.
    private Clip clip(Sound sound) {
        if (unavailable.contains(sound)) return null;
        Clip clip = clips.get(sound);
        if (clip != null) return clip;
        // Decorator (Java library): BufferedInputStream wraps an InputStream and adds buffering
        // while remaining an InputStream. AudioSystem then interprets the buffered audio data.
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(AssetManager.resource(sound.path())))) {
            clip = AudioSystem.getClip();
            clip.open(stream);
            clips.put(sound, clip);
            applyGain(sound, clip);
            return clip;
        } catch (Exception error) {
            unavailable.add(sound);
            System.err.println("Audio unavailable for " + sound + ": " + error.getMessage());
            return null;
        }
    }

    // Combine master volume with this sound's gain; Java Sound's gain control expects decibels.
    private void applyGain(Sound sound, Clip clip) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float linear = volume / 100f * sound.gain();
            float db = linear <= 0 ? control.getMinimum() : (float) (20 * Math.log10(linear));
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
        }
        if (clip.isControlSupported(javax.sound.sampled.BooleanControl.Type.MUTE)) {
            ((javax.sound.sampled.BooleanControl) clip.getControl(javax.sound.sampled.BooleanControl.Type.MUTE)).setValue(volume == 0);
        }
    }

    // Update all cached clips on the same worker that performs playback operations.
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        executor.execute(() -> clips.forEach(this::applyGain));
    }

    // Play a one-shot cue from its beginning; repeated requests restart that sound's cached clip.
    public void play(Sound sound) {
        executor.execute(() -> {
            if (volume == 0) return;
            Clip clip = clip(sound);
            if (clip == null) return;
            clip.stop();
            clip.setFramePosition(0);
            applyGain(sound, clip);
            clip.start();
        });
    }

    // Replace the current background loops. Null stops them without starting a replacement.
    public void ambience(Sound sound) {
        executor.execute(() -> {
            for (Sound loop : new Sound[]{Sound.HUM, Sound.FAN, Sound.MENU_MUSIC, Sound.MENU_STATIC}) {
                Clip old = clips.get(loop);
                if (old != null) old.stop();
            }
            if (sound == null) return;
            Clip clip = clip(sound);
            if (clip != null) {
                clip.setFramePosition(0);
                applyGain(sound, clip);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        });
    }

    /** Decode both tracks first, then start them together; stop both on entering a shift. */
    public void menuMusic() {
        executor.execute(() -> {
            Clip music = clip(Sound.MENU_MUSIC);
            Clip staticNoise = clip(Sound.MENU_STATIC);
            for (Sound loop : new Sound[]{Sound.HUM, Sound.FAN, Sound.MENU_MUSIC, Sound.MENU_STATIC}) {
                Clip old = clips.get(loop);
                if (old != null) old.stop();
            }
            if (music != null) {
                music.setFramePosition(0);
                applyGain(Sound.MENU_MUSIC, music);
            }
            if (staticNoise != null) {
                staticNoise.setFramePosition(0);
                applyGain(Sound.MENU_STATIC, staticNoise);
            }
            if (music != null) music.loop(Clip.LOOP_CONTINUOUSLY);
            if (staticNoise != null) staticNoise.loop(Clip.LOOP_CONTINUOUSLY);
        });
    }

    // Queue clip cleanup after existing audio tasks, then stop accepting new tasks.
    @Override public void close() {
        executor.execute(() -> clips.values().forEach(Clip::close));
        executor.shutdown();
    }
}
