package com.sokoban.util;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;

public class SoundEngine {
    private static Synthesizer synth;
    private static MidiChannel channel;
    private static boolean enabled = true;

    static {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channel = synth.getChannels()[0];
            // Set program/instrument to a clean acoustic piano/synth (0)
            channel.programChange(0); 
        } catch (Exception e) {
            System.err.println("Failed to initialize program SoundEngine: " + e.getMessage());
        }
    }

    public static void toggleSound() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static void playNoteAsync(int note, int velocity, int durationMs) {
        if (!enabled || channel == null) return;
        new Thread(() -> {
            try {
                channel.noteOn(note, velocity);
                Thread.sleep(durationMs);
                channel.noteOff(note);
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }

    public static void playMove() {
        // Soft low pitch thud
        playNoteAsync(42, 60, 50); 
    }

    public static void playPush() {
        // Hollow wooden sound
        playNoteAsync(54, 80, 80);
    }

    public static void playDestinationSnap() {
        // Two consecutive rising shiny notes
        new Thread(() -> {
            try {
                if (!enabled || channel == null) return;
                channel.noteOn(76, 90);
                Thread.sleep(80);
                channel.noteOff(76);
                channel.noteOn(81, 100);
                Thread.sleep(150);
                channel.noteOff(81);
            } catch (Exception e) {}
        }).start();
    }

    public static void playWallBump() {
        // Low dull bounce
        playNoteAsync(33, 50, 100);
    }

    public static void playLevelClear() {
        // Triumphant rising major arpeggio
        new Thread(() -> {
            try {
                if (!enabled || channel == null) return;
                int[] melody = {60, 64, 67, 72};
                int[] durations = {150, 150, 150, 400};
                for (int i = 0; i < melody.length; i++) {
                    channel.noteOn(melody[i], 100);
                    Thread.sleep(durations[i]);
                    channel.noteOff(melody[i]);
                }
            } catch (Exception e) {}
        }).start();
    }
}
