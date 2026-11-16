package com.gudos.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;

/** Synthesized 8-bit sounds: no external audio assets or libraries required. [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests synthesized clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers). */
/*
 * Chiptune
 *
 * Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests synthesized clips);
 * [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers);
 * [ui/SecretPanel.java:67 (SecretPanel)](../ui/SecretPanel.java#L67) (requests secret-game sounds).
 *
 * Purpose: Generates only the hidden minigame audio in memory.
 *
 * Execution flow: create returns an AudioInputStream for SECRET_JUMP, SECRET_PICKUP or
 * SECRET_AMBIENCE. AudioService opens it through the same clip path used for WAVs.
 *
 * Design rationale: Square waves and envelopes create small retro cues without
 * another library. The eight-second drone uses a deterministic noise sequence and quiet endpoints.
 * Current ordinary encounter sounds are bundled WAVs.
 *
 * State: The generated format is 22,050 Hz, signed eight-bit, mono PCM. The phase
 * accumulates frequency/sampleRate; the envelope fades edges.
 *
 * Summary: A null Sound.path means synthesize this secret cue. Other cues load their WAV
 * file; the whole audio system is not synthesized.
 * [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
 */
public final class Chiptune {
    /*
     * Private constructor prevents instantiating this stateless audio generator.
     * [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     * Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests synthesized clips);
     * [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers);
     * [ui/SecretPanel.java:67 (SecretPanel)](../ui/SecretPanel.java#L67) (requests secret-game sounds).
     */
    private Chiptune() { }

    /*
     * Accept only the three secret cues; delegate ambience to its generator. For jump/pickup
     * allocate 8-bit mono PCM, advance square-wave phase at stepped frequencies, fade sample
     * amplitude at the ends and wrap it as AudioInputStream. Ordinary WAV cues are rejected here.
     * [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     * Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips);
     * [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
     */
    public static AudioInputStream create(Sound sound) {
        // These three sounds are generated in memory rather than loaded from bundled WAV files. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
        if (sound == Sound.SECRET_AMBIENCE) return ambience();
        if (sound != Sound.SECRET_JUMP && sound != Sound.SECRET_PICKUP) throw new IllegalArgumentException("Not a secret cue");
        int rate = 22050, count = (int) (rate * (sound == Sound.SECRET_JUMP ? 0.15 : 0.22));
        // Signed 8-bit, mono PCM: each byte is one sample at 22,050 samples per second. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
        byte[] pcm = new byte[count];
        double phase = 0;
        for (int i = 0; i < count; i++) {
            double t = i / (double) rate;
            // Stepped pitches and a short fade keep the cues retro without sharp clicks. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
            double frequency = sound == Sound.SECRET_JUMP ? 330 + 110 * (int) (t / 0.03)
                    : (t < 0.07 ? 440 : t < 0.14 ? 660 : 880);
            // The fractional phase advances through each waveform cycle. Switching between | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
            // positive and negative amplitude at its midpoint produces a square wave. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
            phase += frequency / rate;
            double envelope = Math.min(1, Math.min(i / 100.0, (count - 1 - i) / 180.0));
            pcm[i] = (byte) ((phase % 1 < 0.5 ? 1 : -1) * 55 * Math.max(0, envelope));
        }
        return new AudioInputStream(new ByteArrayInputStream(pcm), new AudioFormat(rate, 8, 1, true, false), count);
    }

    /** Uneasy detuned square-wave drone and low-bit hiss, without notes, beats or a melody. [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers). */
    /*
     * Generate an eight-second signed 8-bit mono drone using two detuned square waves plus
     * deterministic coarse noise. Fade both endpoints toward silence so repeated looping avoids a
     * sharp boundary click.
     * [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31).
     * Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips);
     * [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
     */
    private static AudioInputStream ambience() {
        int rate = 22050, count = rate * 8;
        byte[] pcm = new byte[count];
        double firstPhase = 0, secondPhase = 0;
        // A deterministic seed makes each generated ambience loop repeat consistently. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
        int noise = 0x5a17;
        for (int i = 0; i < count; i++) {
            double seconds = i / (double) rate;
            // Closely spaced, slowly drifting pitches produce an unsettling beating drone. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
            firstPhase += (57 + 1.7 * Math.sin(seconds * .8)) / rate;
            secondPhase += (60.2 + 1.1 * Math.sin(seconds * .47)) / rate;
            // Update the low-resolution noise less often than the sample rate for a gritty hiss. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
            if (i % 320 == 0) noise = (noise * 1103515245 + 12345) & 0x7fffffff;
            // Bring both ends to silence so the audio clip can loop without a sharp seam. | [p. 31: Chiptune](https://github.com/itsnold/FiveNightsAtGudos/blob/86b8cdac1d67c3fd619cb33a56097e1b792164ad/docs/defense/Reviewer_Pages.md#page-31). | Related: [audio/AudioService.java:79 (clip)](AudioService.java#L79) (requests generated clips); [audio/Sound.java:72 (Sound)](Sound.java#L72) (secret cue identifiers).
            double loopEnvelope = Math.min(1, Math.min(seconds * 4, (8 - seconds) * 4));
            double first = firstPhase % 1 < .5 ? 1 : -1;
            double second = secondPhase % 1 < .5 ? 1 : -1;
            double hiss = (noise & 1) == 0 ? -1 : 1;
            pcm[i] = (byte) ((first * 14 + second * 9 + hiss * 3) * Math.max(0, loopEnvelope));
        }
        return new AudioInputStream(new ByteArrayInputStream(pcm), new AudioFormat(rate, 8, 1, true, false), count);
    }
}
