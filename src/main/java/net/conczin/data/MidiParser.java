package net.conczin.data;

import javax.sound.midi.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class MidiParser {
    public static List<Melody.Track> parseMidi(InputStream inputStream) {
        List<Melody.Track> tracks = new LinkedList<>();

        Sequence sequence;
        try {
            sequence = MidiSystem.getSequence(inputStream);
        } catch (InvalidMidiDataException | IOException e) {
            throw new RuntimeException(e);
        }

        // Fetch shared events
        List<MidiEvent> sharedEvents = new LinkedList<>();
        for (Track track : sequence.getTracks()) {
            getEvents(track).stream()
                    .filter(event -> event.getMessage() instanceof MetaMessage m && m.getType() == 0x51)
                    .forEach(sharedEvents::add);
        }

        // Iterate through tracks and MIDI events
        int trackNr = 1;
        for (Track track : sequence.getTracks()) {
            // Merge with shared events and sort
            List<MidiEvent> events = getEvents(track);
            events.addAll(0, sharedEvents);
            events.sort((a, b) -> (int) (a.getTick() - b.getTick()));

            double bpm = 120;
            long lastTick = 0;
            double time = 0;
            String name = "Track " + trackNr;
            List<Melody.Note> notes = new LinkedList<>();
            HashMap<Integer, Melody.Note.Builder> currentNotes = new HashMap<>();

            for (MidiEvent event : events) {
                // Convert notes into ms
                long tick = event.getTick();
                double deltaMs = ((tick - lastTick) * 60000.0) / (sequence.getResolution() * bpm);
                time += deltaMs;
                lastTick = tick;
                int ms = (int) time;

                MidiMessage message = event.getMessage();

                // Parse meta events
                if (message instanceof MetaMessage metaMessage) {
                    byte[] data = metaMessage.getData();
                    int type = metaMessage.getType();
                    if (type == 0x03) {
                        String newName = new String(data).strip();
                        if (!newName.isEmpty()) {
                            name = newName;
                        }
                    } else if (type == 0x51) {
                        int microsecondsPerBeat = ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF);
                        bpm = Math.round(60000000.0f / microsecondsPerBeat);
                    }
                }

                // Parse note on/off events
                if (message instanceof ShortMessage sm) {
                    int command = sm.getCommand();

                    // Another way to decode note offs is note ons with velocity 0
                    if (command == ShortMessage.NOTE_ON && sm.getData2() == 0) {
                        command = ShortMessage.NOTE_OFF;
                    }

                    if (command == ShortMessage.NOTE_ON) {
                        int note = sm.getData1();
                        int velocity = sm.getData2();

                        currentNotes.put(note, new Melody.Note.Builder(note, velocity, ms));
                    } else if (command == ShortMessage.NOTE_OFF) {
                        int note = sm.getData1();
                        Melody.Note.Builder noteBuilder = currentNotes.get(note);
                        currentNotes.remove(note);
                        if (noteBuilder != null) {
                            noteBuilder.length = ms - noteBuilder.time;
                            notes.add(noteBuilder.build());
                        }
                    }
                }
            }

            if (!notes.isEmpty()) {
                trackNr += 1;

                // Sort
                notes.sort(Comparator.comparingInt(Melody.Note::time));

                tracks.add(new Melody.Track(name, notes));
            }
        }

        // Find first note
        int offset = Integer.MAX_VALUE;
        for (Melody.Track track : tracks) {
            List<Melody.Note> notes = track.notes();
            if (!notes.isEmpty()) {
                offset = Math.min(offset, notes.getFirst().time());
            }
        }

        // And average velocity
        int totalVelocity = 0;
        int totalNotes = 0;
        for (Melody.Track track : tracks) {
            for (Melody.Note note : track.notes()) {
                totalVelocity += note.velocity();
                totalNotes += 1;
            }
        }
        float averageVelocity = (float) totalVelocity / (float) totalNotes;

        // And offset all notes
        for (Melody.Track track : tracks) {
            List<Melody.Note> newNotes = new LinkedList<>();
            for (Melody.Note note : track.notes()) {
                newNotes.add(new Melody.Note(
                        note.note(),
                        (int) (note.velocity() / averageVelocity * 64),
                        note.time() - offset,
                        note.length()
                ));
            }
            track.setNotes(newNotes);
        }

        return tracks;
    }

    private static List<MidiEvent> getEvents(Track track) {
        List<MidiEvent> events = new LinkedList<>();
        for (int i = 0; i < track.size(); i++) {
            events.add(track.get(i));
        }
        return events;
    }
}
