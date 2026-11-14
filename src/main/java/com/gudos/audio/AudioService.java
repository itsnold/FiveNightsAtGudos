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
import java.util.Arrays;
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
    private Set<Sound> requestedLoops = Set.of();

    public AudioService(int volume) { this.volume = Math.max(0, Math.min(100, volume)); }

    // Find an already-open clip, or decode its bundled resource the first time it is requested.
    private Clip clip(Sound sound) {
        if (unavailable.contains(sound)) return null;
        Clip clip = clips.get(sound);
        if (clip != null) return clip;
        // Decorator (Java library): BufferedInputStream wraps an InputStream and adds buffering
        // while remaining an InputStream. AudioSystem then interprets the buffered audio data.
        // A null path identifies a synthesized minigame sound; other sounds use bundled WAVs.
        try (AudioInputStream stream = sound.path() == null ? Chiptune.create(sound)
                : AudioSystem.getAudioInputStream(new BufferedInputStream(AssetManager.resource(sound.path())))) {
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
        play(sound, 0);
    }

    /** Position encounter cues toward the approach; UI feedback stays centered. */
    public void play(Sound sound, float pan) {
        executor.execute(() -> {
            if (volume == 0) return;
            if (sound == Sound.GUDO_RETREAT) {
                // The retreat replaces any still-playing approach/attack cue, including the long step sequence.
                for (Sound threat : new Sound[]{Sound.GUDO_STEP, Sound.GUDO_WARNING, Sound.GUDO_POUND,
                        Sound.GUDO_WHISPER, Sound.GUDO_RUSH}) {
                    Clip old = clips.get(threat);
                    if (old != null) old.stop();
                }
            }
            Clip clip = clip(sound);
            if (clip == null) return;
            clip.stop();
            clip.setFramePosition(0);
            applyGain(sound, clip);
            applyPan(clip, pan);
            clip.start();
        });
    }

    /** Stop every scene layer and one-shot, including the minigame's synthesized ambience. */
    public void stopAll() {
        requestedLoops = Set.of();
        executor.execute(() -> clips.values().forEach(Clip::stop));
    }

    private void applyPan(Clip clip, float pan) {
        FloatControl.Type type = clip.isControlSupported(FloatControl.Type.PAN)
                ? FloatControl.Type.PAN : FloatControl.Type.BALANCE;
        if (clip.isControlSupported(type)) {
            FloatControl control = (FloatControl) clip.getControl(type);
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), pan)));
        }
    }

    /** Replace only changed loops, preserving playback positions of shared layers. */
    public void ambience(Sound sound, Sound... layers) {
        Set<Sound> next = EnumSet.noneOf(Sound.class);
        if (sound != null) next.add(sound);
        next.addAll(Arrays.asList(layers));
        if (next.equals(requestedLoops)) return;
        Set<Sound> previous = requestedLoops;
        requestedLoops = Set.copyOf(next);
        executor.execute(() -> {
            for (Sound loop : previous) {
                if (next.contains(loop)) continue;
                Clip old = clips.get(loop);
                if (old != null) old.stop();
            }
            for (Sound loop : next) {
                if (previous.contains(loop)) continue;
                Clip clip = clip(loop);
                if (clip != null) {
                    clip.setFramePosition(0);
                    applyGain(loop, clip);
                    applyPan(clip, 0);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            }
        });
    }

    /** Stop transient sounds on pause, scene exit and before the result sting. */
    public void stopEffects() {
        Set<Sound> loops = requestedLoops;
        executor.execute(() -> {
            clips.forEach((sound, clip) -> { if (!loops.contains(sound)) clip.stop(); });
        });
    }

    public void menuMusic() { ambience(Sound.MENU_MUSIC, Sound.MENU_STATIC); }

    /** Breathing is local to an office arrival; hiss belongs only to the raised monitor. */
    public void gameplay(boolean monitor, boolean atOffice, boolean tense, float approachPan, Sound cameraGarble) {
        Set<Sound> loops = EnumSet.of(Sound.FAN, Sound.ROOM_TONE);
        if (monitor) loops.add(Sound.CAMERA_HISS);
        // Both layers belong to the currently viewed disrupted feed; looking away removes both immediately.
        if (monitor && cameraGarble != null) {
            loops.add(cameraGarble);
            loops.add(Sound.CAMERA_DIGITAL);
        }
        if (atOffice) loops.add(Sound.GUDO_BREATH);
        if (tense) loops.add(Sound.TENSION);
        ambience(Sound.FAN, loops.stream().filter(sound -> sound != Sound.FAN).toArray(Sound[]::new));
        if (atOffice) executor.execute(() -> {
            Clip breath = clips.get(Sound.GUDO_BREATH);
            if (breath != null) applyPan(breath, approachPan);
        });
    }

    // Queue clip cleanup after existing audio tasks, then stop accepting new tasks.
    @Override public void close() {
        executor.execute(() -> clips.values().forEach(Clip::close));
        executor.shutdown();
    }
}
