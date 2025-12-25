package net.conczin.data;

import com.hypixel.hytale.codec.Codec;
import net.conczin.utils.ListCodec;
import net.conczin.utils.RecordCodec;

import java.util.Collections;
import java.util.List;

public record Melody(String name, List<Track> tracks) {
    public static final RecordCodec<Melody> CODEC = RecordCodec.composite(
            "Name", Codec.STRING, Melody::name,
            "Tracks", new ListCodec<>(Track.CODEC), Melody::tracks,
            Melody::new
    );


    public int getLength() {
        int length = 0;
        for (Track track : tracks) {
            length = Math.max(length, track.getLength());
        }
        return length;
    }

    public record Track(String name, List<Note> notes) {
        public static final RecordCodec<Track> CODEC = RecordCodec.composite(
                "Name", Codec.STRING, Track::name,
                "Notes", new ListCodec<>(Note.CODEC), Track::notes,
                Track::new
        );


        @Override
        public List<Note> notes() {
            return Collections.unmodifiableList(notes);
        }

        public int getLength() {
            if (notes.isEmpty()) return 0;
            Note note = notes.getLast();
            return note.time() + note.length();
        }

        public void setNotes(List<Note> notes) {
            this.notes.clear();
            this.notes.addAll(notes);
        }
    }


    public record Note(int note, int velocity, int time, int length) {
        public static final RecordCodec<Note> CODEC = RecordCodec.composite(
                "Note", Codec.INTEGER, Note::note,
                "Velocity", Codec.INTEGER, Note::velocity,
                "Time", Codec.INTEGER, Note::time,
                "Length", Codec.INTEGER, Note::time,
                Note::new
        );

        public static class Builder {
            public final int note;
            public final int velocity;
            public final int time;
            public int length;

            public Builder(int note, int velocity, int time) {
                this.note = note;
                this.velocity = velocity;
                this.time = time;
            }

            public Note build() {
                return new Note(note, velocity, time, length);
            }
        }
    }
}
