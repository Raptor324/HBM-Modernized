package api.hbm_m.entity;

public interface IRadarDetectable {

    enum RadarTargetType {
        MISSILE_TIER0("radar.target.tier0", "Micro Missile"),
        MISSILE_TIER1("radar.target.tier1", "Tier 1 Missile"),
        MISSILE_TIER2("radar.target.tier2", "Tier 2 Missile"),
        MISSILE_TIER3("radar.target.tier3", "Tier 3 Missile"),
        MISSILE_TIER4("radar.target.tier4", "Tier 4 Missile"),
        MISSILE_10("radar.target.custom10", "Size 10 Custom Missile"),
        MISSILE_10_15("radar.target.custom1015", "Size 10/15 Custom Missile"),
        MISSILE_15("radar.target.custom15", "Size 15 Custom Missile"),
        MISSILE_15_20("radar.target.custom1520", "Size 15/20 Custom Missile"),
        MISSILE_20("radar.target.custom20", "Size 20 Custom Missile"),
        MISSILE_AB("radar.target.abm", "Anti-Ballistic Missile"),
        MIRVLET("radar.target.mirvlet", "Mirvlet"),
        PLAYER("radar.target.player", "Player"),
        ARTILLERY("radar.target.artillery", "Artillery Shell"),
        DOOMSDAY("radar.target.doomsday", "Doomsday Missile"),
        DROPSHIP("radar.target.dropship", "Cargo Dropship"),
        METEOR("radar.target.meteor", "Meteor"),
        SHUTTLE("radar.target.shuttle", "Reliant Robin Space Shuttle");

        public final String translationKey;
        public final String name;

        RadarTargetType(String translationKey, String name) {
            this.translationKey = translationKey;
            this.name = name;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    RadarTargetType getTargetType();

    /** 1:1 IRadarDetectableNT#canBeSeenBy: ob das Objekt von diesem Radar/Geschuetz gesehen werden kann (Tarnung). */
    default boolean canBeSeenBy(Object radar) {
        return true;
    }
}
