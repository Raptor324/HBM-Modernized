package com.hbm_m.util;

/**
 * 1:1 {@code com.hbm.util.NoteBuilder}: Notenfolgen als RTTY-Signal ("Instrument:Note:Oktave", durch "-" getrennt),
 * dazu {@code RTTYSystem.getTestSender} (Song of Storms auf dem Kanal "2012-08-06").
 */
public class NoteBuilder {

    private String beat = "";

    public static NoteBuilder start() {
        return new NoteBuilder();
    }

    public NoteBuilder add(Instrument instrument, Note note, Octave octave) {
        if (!beat.isEmpty())
            beat += "-";

        String result = instrument.ordinal() + ":" + note.ordinal() + ":" + octave.ordinal();

        beat += result;

        return this;
    }

    public String end() {
        return beat;
    }

    public record Hit(Instrument instrument, Note note, Octave octave) { }

    public static Hit[] translate(String beat) {
        String[] hits = beat.split("-");
        Hit[] notes = new Hit[hits.length];

        try {
            for (int i = 0; i < hits.length; i++) {
                String[] components = hits[i].split(":");
                Instrument instrument = Instrument.values()[Integer.parseInt(components[0])];
                Note note = Note.values()[Integer.parseInt(components[1])];
                Octave octave = Octave.values()[Integer.parseInt(components[2])];

                notes[i] = new Hit(instrument, note, octave);
            }

            return notes;
        } catch (Exception ex) {
            return new Hit[0];
        }
    }

    public enum Instrument {
        PIANO,
        BASSDRUM,
        SNARE,
        CLICKS,
        BASSGUITAR;
    }

    public enum Note {
        F_SHARP,
        G,
        G_SHARP,
        A,
        A_SHARP,
        B,
        C,
        C_SHARP,
        D,
        D_SHARP,
        E,
        F;
    }

    public enum Octave {
        LOW, MID, HIGH;
    }

    /* Song of Storms at 300 BPM */
        public static Object getTestSender(long timeStamp) {
    
            int tempo = 4;
            int time = (int) (timeStamp % (tempo * 160));
            
            Instrument flute = Instrument.PIANO;
            Instrument accordion = Instrument.BASSGUITAR;
    
            if(time == tempo * 0) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
            if(time == tempo * 2) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            if(time == tempo * 4) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            
            if(time == tempo * 6) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
            if(time == tempo * 8) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
            
            if(time == tempo * 12) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
            if(time == tempo * 14) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
            if(time == tempo * 16) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
            
            if(time == tempo * 18) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
            if(time == tempo * 20) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
            
            if(time == tempo * 24) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
            if(time == tempo * 26) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            if(time == tempo * 28) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            //
            if(time == tempo * 30) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
            if(time == tempo * 32) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
            
            if(time == tempo * 36) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
            if(time == tempo * 38) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            if(time == tempo * 40) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            
            if(time == tempo * 42) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
            if(time == tempo * 44) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).end();
    
            if(time == tempo * 48) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 50) return NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 52) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(flute, Note.D, Octave.MID).end();
            if(time == tempo * 54) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW);
    
            if(time == tempo * 56) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 58) return NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 60) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).add(flute, Note.D, Octave.MID).end();
    
            if(time == tempo * 64) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.E, Octave.MID).end();
            if(time == tempo * 66) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
            if(time == tempo * 67) return NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
            if(time == tempo * 68) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).add(flute, Note.E, Octave.MID).end();
            if(time == tempo * 69) return NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
            //
            if(time == tempo * 70) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.E, Octave.MID).end();
            if(time == tempo * 71) return NoteBuilder.start().add(flute, Note.B, Octave.MID).end();
            if(time == tempo * 72) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.LOW).add(flute, Note.A, Octave.MID).end();
    
            if(time == tempo * 76) return NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
            if(time == tempo * 78) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 80) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 81) return NoteBuilder.start().add(flute, Note.G, Octave.MID).end();
    
            if(time == tempo * 82) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.A, Octave.MID).end();
            if(time == tempo * 84) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
            
            if(time == tempo * 88) return NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
            if(time == tempo * 90) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 92) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 93) return NoteBuilder.start().add(accordion, Note.B, Octave.MID).add(flute, Note.G, Octave.MID).end();
    
            if(time == tempo * 94) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(flute, Note.E, Octave.LOW).end();
            if(time == tempo * 96) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).end();
    
            if(time == tempo * 100) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 101) return NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 102) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.MID).end();
            if(time == tempo * 104) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.B, Octave.MID).end();
            //
            if(time == tempo * 106) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 107) return NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 108) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.D, Octave.MID).end();
    
            if(time == tempo * 112) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(flute, Note.E, Octave.MID).end();
            if(time == tempo * 114) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).end();
            if(time == tempo * 115) return NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
            if(time == tempo * 116) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.LOW).add(accordion, Note.C, Octave.MID).add(flute, Note.E, Octave.MID).end();
            if(time == tempo * 117) return NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
    
            if(time == tempo * 118) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.E, Octave.MID).end();
            if(time == tempo * 119) return NoteBuilder.start().add(flute, Note.C, Octave.MID).end();
            if(time == tempo * 120) return NoteBuilder.start().add(accordion, Note.E, Octave.LOW).add(accordion, Note.G, Octave.LOW).add(accordion, Note.B, Octave.MID).add(flute, Note.A, Octave.MID).end();
    
            if(time == tempo * 124) return NoteBuilder.start().add(accordion, Note.G, Octave.LOW).add(flute, Note.A, Octave.MID).end();
            if(time == tempo * 126) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 128) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).add(flute, Note.F, Octave.LOW).end();
            if(time == tempo * 129) return NoteBuilder.start().add(flute, Note.G, Octave.MID).end();
    
            if(time == tempo * 130) return NoteBuilder.start().add(accordion, Note.F, Octave.LOW).add(flute, Note.A, Octave.MID).end();
            if(time == tempo * 132) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(accordion, Note.E, Octave.LOW).add(accordion, Note.A, Octave.MID).add(accordion, Note.G, Octave.LOW).end();
            if(time == tempo * 134) return NoteBuilder.start().add(flute, Note.A, Octave.MID).end();
    
            if(time == tempo * 136) return NoteBuilder.start().add(accordion, Note.C, Octave.LOW).add(flute, Note.D, Octave.LOW).end();
            if(time == tempo * 138) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).end();
            if(time == tempo * 140) return NoteBuilder.start().add(accordion, Note.D, Octave.LOW).add(accordion, Note.F, Octave.LOW).add(accordion, Note.A, Octave.MID).end();
    
            
            return "";
        }
}
