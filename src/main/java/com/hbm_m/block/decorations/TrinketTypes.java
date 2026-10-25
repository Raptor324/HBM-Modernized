package com.hbm_m.block.decorations;

/**
 * Die Typen der Sammelfiguren des Originals: {@code BlockBobble.BobbleType}, {@code BlockSnowglobe.SnowglobeType},
 * {@code BlockPlushie.PlushieType} (Werte, Reihenfolge und Texte 1:1; die Ordnungszahl steht im Gegenstand/Block).
 */
public final class TrinketTypes {

    private TrinketTypes() {}

    public enum BobbleType {
        NONE("null", "null", null, null, false, "board_blank"),
        STRENGTH("Strength", "Strength", null, "It's essential to give your arguments impact.", false, "bridge_bios"),
        PERCEPTION("Perception", "Perception", null, "Only through observation will you perceive weakness.", false, "bridge_north"),
        ENDURANCE("Endurance", "Endurance", null, "Always be ready to take one for the team.", false, "bridge_south"),
        CHARISMA("Charisma", "Charisma", null, "Nothing says pizzaz like a winning smile.", false, "bridge_io"),
        INTELLIGENCE("Intelligence", "Intelligence", null, "It takes the smartest individuals to realize$there's always more to learn.", false, "bridge_bus"),
        AGILITY("Agility", "Agility", null, "Never be afraid to dodge the sensitive issues.", false, "bridge_chipset"),
        LUCK("Luck", "Luck", null, "There's only one way to give 110%.", false, "bridge_cmos"),
        BOB("Robert \"The Bobcat\" Katzinsky", "HbMinecraft", "Hbm's Nuclear Tech Mod", "I know where you live, " + System.getProperty("user.name"), false, "cpu_socket"),
        FRIZZLE("Frooz", "Frooz", "Weapon models", "BLOOD IS FUEL", true, "cpu_clock"),
        PU238("Pu-238", "Pu-238", "Improved Tom impact mechanics", null, false, "cpu_register"),
        VT("VT-6/24", "VT-6/24", "Balefire warhead model and general texturework", "You cannot unfuck a horse.", true, "cpu_ext"),
        DOC("The Doctor", "Doctor17PH", "Russian localization, lunar miner", "Perhaps the moon rocks were too expensive", true, "cpu_cache"),
        BLUEHAT("The Blue Hat", "The Blue Hat", "Textures", "payday 2's deagle freeaim champ of the year 2022", true, "mem_16k_a"),
        PHEO("Pheo", "Pheonix", "Deuterium machines, tantalium textures, Reliant Rocket", "RUN TO THE BEDROOM, ON THE SUITCASE ON THE LEFT,$YOU'LL FIND MY FAVORITE AXE", true, "mem_16k_b"),
        ADAM29("Adam29", "Adam29", "Ethanol, liquid petroleum gas", "You know, nukes are really quite beatiful.$It's like watching a star be born for a split second.", true, "mem_16k_c"),
        UFFR("UFFR", "UFFR", "All sorts of things from his PR", "fried shrimp", false, "mem_socket"),
        VAER("vaer", "vaer", "ZIRNOX", "taken de family out to the weekend cigarette festival", true, "mem_16k_d"),
        NOS("Dr Nostalgia", "Dr Nostalgia", "SSG and Vortex models", "Take a picture, I'ma pose, paparazzi$I've been drinking, moving like a zombie", true, "board_transistor"),
        DRILLGON("Drillgon200", "Drillgon200", "1.12 Port", null, false, "cpu_logic"),
        CIRNO("Cirno", "Cirno", "the only multi layered skin i had", "No brain. Head empty.", true, "board_blank"),
        MICROWAVE("Microwave", "Microwave", "OC Compatibility and massive RBMK/packet optimizations", "they call me the food heater$john optimization", true, "board_converter"),
        PEEP("Peep", "LePeeperSauvage", "Coilgun, Leadburster and Congo Lake models, BDCL QC", "Fluffy ears can't hide in ash, nor snow.", true, "card_board"),
        MELLOW("MELLOWARPEGGIATION", "Mellow", "NBT Structures, industrial lighting, animation tools", "Make something cool now, ask for permission later.", true, "card_processor"),
        ABEL("Abel1502", "Abel1502", "Abilities GUI, optimizations and many QoL improvements", "NANTO SUBARASHII", true, "cpu_register");

        /** Titel im Fenster. */
        public final String title;
        /** Gravur am Sockel. */
        public final String label;
        public final String contribution;
        public final String inscription;
        public final boolean skinLayers;
        /** {@code ScrapType} fuer den Schredder (Plastikschrott-Untertyp). */
        public final String scrap;

        BobbleType(String title, String label, String contribution, String inscription, boolean layers, String scrap) {
            this.title = title;
            this.label = label;
            this.contribution = contribution;
            this.inscription = inscription;
            this.skinLayers = layers;
            this.scrap = scrap;
        }
    }

    public enum SnowglobeType {
        NONE("NONE", null),
        RIVETCITY("Rivet City", "Welcome to Rivet City. Please wait while the bridge extends."),
        TENPENNYTOWER("Tenpenny Tower", "Tenpenny Tower is the brainchild of Allistair Tenpenny, a British refugee who came to the Capital Wasteland seeking his fortune."),
        LUCKY38("Lucky 38", "My guess? Leads to a big cashout at some casino - and if the \"38\" on it is any indication... well... Lucky 38 it is."),
        SIERRAMADRE("Sierra Madre", "It's the moment you've been waiting for, the reason we're all here - the Gala Event, the Grand Opening of the Sierra Madre Casino."),
        PRYDWEN("The Prydwen", "People of the Commonwealth. Do not interfere. Our intentions are peaceful. We are the Brotherhood of Steel.");

        public final String label;
        public final String inscription;

        SnowglobeType(String label, String inscription) {
            this.label = label;
            this.inscription = inscription;
        }
    }

    public enum PlushieType {
        NONE("NONE", null),
        YOMI("Yomi", "Hi! Can I be your rabbit friend?"),
        NUMBERNINE("Number Nine", "None of y'all deserve coal."),
        HUNDUN("Hundun", "混沌"),
        DERG("Dragon", "Squeeze him.");

        public final String label;
        public final String inscription;

        PlushieType(String label, String inscription) {
            this.label = label;
            this.inscription = inscription;
        }
    }

    public static <E extends Enum<E>> E safe(Class<E> cls, int ordinal) {
        E[] v = cls.getEnumConstants();
        return v[Math.abs(ordinal) % v.length];
    }
}
