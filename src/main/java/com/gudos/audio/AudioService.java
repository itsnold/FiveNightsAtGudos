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

/** Audio decoding and mixer calls never block Swing. Missing hardware degrades to silence. [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (requests gameplay audio layers); [audio/Sound.java:72 (Sound)](Sound.java#L72) (cue paths and gains). */
/*
 * AudioService
 *
 * Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (requests gameplay audio layers);
 * [audio/Sound.java:72 (Sound)](Sound.java#L72) (cue paths and gains);
 * [audio/Chiptune.java:49 (create)](Chiptune.java#L49) (synthesized secret-game cues).
 *
 * Purpose: Provides the audio facade over Java Sound and a serial audio worker.
 *
 * Execution flow: Application or a panel callback requests play, ambience, menuMusic or gameplay.
 * The worker lazily decodes/caches a Clip, sets gain/pan, starts or loops it, and later stops/closes
 * it.
 *
 * Design rationale: One facade hides stream decoding, mixer access, caching, thread
 * scheduling, layers and cleanup. Missing hardware is remembered as unavailable and gameplay
 * continues. The worker is daemon; the storage worker is not.
 *
 * State: clips caches by Sound; unavailable remembers failed cues; volatile volume is
 * shared with the worker; requestedLoops records desired scene layers. Runtime audio requests are
 * expected from the coordinator/UI thread.
 *
 * Summary: The client asks for a sound rather than manually creating AudioInputStream, Clip,
 * FloatControl and loop logic. That is the facade boundary.
 * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
 */
public final class AudioService implements AutoCloseable {
    // Facade: callers request sounds through play/ambience/menuMusic instead of managing Java Sound. | [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [audio/Sound.java:72 (Sound)](Sound.java#L72) (cue paths and gains); [Application.java:391 (updateGameAudio)](../Application.java#L391) (requests gameplay audio layers).
    // One worker owns clip operations; its daemon thread does not keep the JVM alive on its own. | [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (requests gameplay audio layers); [audio/Sound.java:72 (Sound)](Sound.java#L72) (cue paths and gains).
    private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "gudo-audio");
        thread.setDaemon(true);
        return thread;
    });
    // Load each sound lazily and reuse its clip. Failed sounds are remembered to avoid repeated errors. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (requests gameplay audio layers); [audio/Sound.java:72 (Sound)](Sound.java#L72) (cue paths and gains).
    private final Map<Sound, Clip> clips = new EnumMap<>(Sound.class);
    private final Set<Sound> unavailable = EnumSet.noneOf(Sound.class);
    private volatile int volume;
    private Set<Sound> requestedLoops = Set.of();

    /*
     * Clamp the initial master volume into 0..100. Clip decoding stays lazy, so constructing this
     * facade does not eagerly open every WAV or require a functioning mixer.
     * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:120 (AudioService call)](../Application.java#L120) (caller of AudioService);
     * [audio/Sound.java:72 (Sound)](Sound.java#L72) (cue paths and gains);
     * [audio/Chiptune.java:49 (create)](Chiptune.java#L49) (synthesized secret-game cues).
     */
    public AudioService(int volume) { this.volume = Math.max(0, Math.min(100, volume)); }

    // Find an already-open clip, or decode its bundled resource the first time it is requested. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path); [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain).
    /*
     * Return a cached Clip or lazily decode a bundled WAV/Chiptune stream and open one. Cache
     * success; remember failure in unavailable and return null so missing audio hardware does not
     * stop gameplay. BufferedInputStream is a Java-library decorator; the custom renderer decorators
     * are separate project evidence.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path);
     * [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain);
     * [audio/Chiptune.java:49 (create)](Chiptune.java#L49) (generated hidden-game cues).
     */
    private Clip clip(Sound sound) {
        if (unavailable.contains(sound)) return null;
        Clip clip = clips.get(sound);
        if (clip != null) return clip;
        // Decorator (Java library): BufferedInputStream wraps an InputStream and adds buffering | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path); [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain).
        // while remaining an InputStream. AudioSystem then interprets the buffered audio data. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path); [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain).
        // A null path identifies a synthesized minigame sound; other sounds use bundled WAVs. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path); [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain).
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

    // Combine master volume with this sound's gain; Java Sound's gain control expects decibels. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path); [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain).
    /*
     * Combine volume/100 with the cue gain, convert positive linear gain using 20*log10, and clamp
     * to the mixer control range. If MUTE exists set it for zero volume. Unsupported hardware
     * controls are skipped.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path);
     * [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain);
     * [audio/Chiptune.java:49 (create)](Chiptune.java#L49) (generated hidden-game cues).
     */
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

    // Update all cached clips on the same worker that performs playback operations. | [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:621 (saveSettings)](../Application.java#L621) (applies changed volume); [ui/SettingsPanel.java:245 (values)](../ui/SettingsPanel.java#L245) (collects volume preference).
    /*
     * Clamp/store the new master value, then enqueue gain updates for every cached clip on the audio
     * worker. The volatile field makes the latest value visible across threads.
     * [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:621 (saveSettings)](../Application.java#L621) (applies changed volume);
     * [ui/SettingsPanel.java:245 (values)](../ui/SettingsPanel.java#L245) (collects volume preference).
     */
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        executor.execute(() -> clips.forEach(this::applyGain));
    }

    // Play a one-shot cue from its beginning; repeated requests restart that sound's cached clip. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:358 (startNight)](../Application.java#L358) (supplies the normal sound callback); [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (encounter cue requests).
    /*
     * The one-argument overload forwards with centered pan. The worker-based overload skips muted
     * requests, stops stale approach cues on retreat, obtains the cached clip, restarts it from
     * frame zero, applies gain/pan and starts it. This is a facade call over several Java Sound
     * operations.
     * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:358 (startNight)](../Application.java#L358) (supplies the normal sound callback);
     * [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (encounter cue requests);
     * [ui/SecretPanel.java:67 (SecretPanel)](../ui/SecretPanel.java#L67) (hidden-game action cue requests).
     */
    public void play(Sound sound) {
        play(sound, 0);
    }

    /** Position encounter cues toward the approach; UI feedback stays centered. [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). Related: [Application.java:358 (startNight)](../Application.java#L358) (supplies the normal sound callback); [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (encounter cue requests). */
    /*
     * The one-argument overload forwards with centered pan. The worker-based overload skips muted
     * requests, stops stale approach cues on retreat, obtains the cached clip, restarts it from
     * frame zero, applies gain/pan and starts it. This is a facade call over several Java Sound
     * operations.
     * [p. 13: Facade](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-13); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:358 (startNight)](../Application.java#L358) (supplies the normal sound callback);
     * [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (encounter cue requests);
     * [ui/SecretPanel.java:67 (SecretPanel)](../ui/SecretPanel.java#L67) (hidden-game action cue requests).
     */
    public void play(Sound sound, float pan) {
        executor.execute(() -> {
            if (volume == 0) return;
            if (sound == Sound.GUDO_RETREAT) {
                // The retreat replaces any still-playing approach/attack cue, including the long step sequence. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:358 (startNight)](../Application.java#L358) (supplies the normal sound callback); [ui/GamePanel.java:393 (tick)](../ui/GamePanel.java#L393) (encounter cue requests).
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

    /** Stop every scene layer and one-shot, including the minigame's synthesized ambience. [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). Related: [Application.java:408 (openSecret)](../Application.java#L408) (normal/hidden audio transition); [Application.java:538 (finishShift)](../Application.java#L538) (ending audio transition). */
    /*
     * Clear the requested loop set and queue stopping every cached clip, including secret ambience
     * and transient cues. It keeps the decoded clips available for later reuse.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:408 (openSecret)](../Application.java#L408) (normal/hidden audio transition);
     * [Application.java:538 (finishShift)](../Application.java#L538) (ending audio transition);
     * [ui/SecretPanel.java:121 (silenceOnOutcome)](../ui/SecretPanel.java#L121) (hidden-ending silence).
     */
    public void stopAll() {
        requestedLoops = Set.of();
        executor.execute(() -> clips.values().forEach(Clip::stop));
    }

    /*
     * Prefer PAN and fall back to BALANCE if supported; clamp the requested -1..1-style position to
     * the actual control bounds. Unsupported positional audio simply remains centered.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [audio/Sound.java:81 (path)](Sound.java#L81) (cue resource path);
     * [audio/Sound.java:90 (gain)](Sound.java#L90) (per-cue gain);
     * [audio/Chiptune.java:49 (create)](Chiptune.java#L49) (generated hidden-game cues).
     */
    private void applyPan(Clip clip, float pan) {
        FloatControl.Type type = clip.isControlSupported(FloatControl.Type.PAN)
                ? FloatControl.Type.PAN : FloatControl.Type.BALANCE;
        if (clip.isControlSupported(type)) {
            FloatControl control = (FloatControl) clip.getControl(type);
            control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), pan)));
        }
    }

    /** Replace only changed loops, preserving playback positions of shared layers. [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). Related: [Application.java:408 (openSecret)](../Application.java#L408) (normal/hidden audio transition); [Application.java:538 (finishShift)](../Application.java#L538) (ending audio transition). */
    /*
     * Build the requested set of loops; return if unchanged. Stop only removed loops and start only
     * added ones on the worker, preserving the playback position of shared layers. Passing null with
     * no extra layers means no ambience loops.
     * [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:408 (openSecret)](../Application.java#L408) (normal/hidden audio transition);
     * [Application.java:538 (finishShift)](../Application.java#L538) (ending audio transition);
     * [ui/SecretPanel.java:121 (silenceOnOutcome)](../ui/SecretPanel.java#L121) (hidden-ending silence).
     */
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

    /** Stop transient sounds on pause, scene exit and before the result sting. [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). Related: [Application.java:408 (openSecret)](../Application.java#L408) (normal/hidden audio transition); [Application.java:538 (finishShift)](../Application.java#L538) (ending audio transition). */
    /*
     * Capture the current desired loops, then stop cached sounds outside that set. This removes
     * transient cues without restarting or stopping ambience shared by the current scene.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:408 (openSecret)](../Application.java#L408) (normal/hidden audio transition);
     * [Application.java:538 (finishShift)](../Application.java#L538) (ending audio transition);
     * [ui/SecretPanel.java:121 (silenceOnOutcome)](../ui/SecretPanel.java#L121) (hidden-ending silence).
     */
    public void stopEffects() {
        Set<Sound> loops = requestedLoops;
        executor.execute(() -> {
            clips.forEach((sound, clip) -> { if (!loops.contains(sound)) clip.stop(); });
        });
    }

    /*
     * Request menu darkness music plus static through ambience. The client does not manage two Clips
     * or looping details.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:262 (showMenu)](../Application.java#L262) (title-music caller);
     * [audio/Sound.java:72 (Sound)](Sound.java#L72) (MENU_MUSIC cue definition).
     */
    public void menuMusic() { ambience(Sound.MENU_MUSIC, Sound.MENU_STATIC); }

    /** Breathing is local to an office arrival; hiss belongs only to the raised monitor. [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (gameplay-layer caller); [ui/GamePanel.java:295 (cameraGarbleSound)](../ui/GamePanel.java#L295) (active feed-disruption cue). */
    /*
     * Build the desired room layers from monitor/office/tension/garble state, then pass them to
     * ambience. Camera garble brings both its variant and digital layer; office breathing is panned
     * toward the approach. Shared loops are not restarted every frame.
     * [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (gameplay-layer caller);
     * [ui/GamePanel.java:295 (cameraGarbleSound)](../ui/GamePanel.java#L295) (active feed-disruption cue).
     */
    public void gameplay(boolean monitor, boolean atOffice, boolean tense, float approachPan, Sound cameraGarble) {
        Set<Sound> loops = EnumSet.of(Sound.FAN, Sound.ROOM_TONE);
        if (monitor) loops.add(Sound.CAMERA_HISS);
        // Both layers belong to the currently viewed disrupted feed; looking away removes both immediately. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:391 (updateGameAudio)](../Application.java#L391) (gameplay-layer caller); [ui/GamePanel.java:295 (cameraGarbleSound)](../ui/GamePanel.java#L295) (active feed-disruption cue).
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

    // Queue clip cleanup after existing audio tasks, then stop accepting new tasks. | [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30). | Related: [Application.java:737 (close)](../Application.java#L737) (orderly cleanup caller); [audio/Sound.java:72 (Sound)](Sound.java#L72) (cached cue identifiers).
    /*
     * Queue closing every cached Clip after already queued work, then shut down the executor. Audio
     * resource cleanup is still owned by the same worker.
     * [p. 16: threads and audio](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-16); [p. 30: AudioService](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-30).
     * Related: [Application.java:737 (close)](../Application.java#L737) (orderly cleanup caller);
     * [audio/Sound.java:72 (Sound)](Sound.java#L72) (cached cue identifiers).
     */
    @Override public void close() {
        executor.execute(() -> clips.values().forEach(Clip::close));
        executor.shutdown();
    }
}
