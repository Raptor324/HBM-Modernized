package api.hbm.entity;

public interface IRadarDetectable {

    enum RadarTargetType {
        MISSILE_TIER0("Micro Missile"),
        MISSILE_TIER1("Tier 1 Missile"),
        MISSILE_TIER2("Tier 2 Missile"),
        MISSILE_TIER3("Tier 3 Missile"),
        MISSILE_TIER4("Tier 4 Missile"),
        MISSILE_10("Size 10 Custom Missile"),
        MISSILE_10_15("Size 10/15 Custom Missile"),
        MISSILE_15("Size 15 Custom Missile"),
        MISSILE_15_20("Size 15/20 Custom Missile"),
        MISSILE_20("Size 20 Custom Missile"),
        MISSILE_AB("Anti-Ballistic Missile"),
        MIRVLET("Mirvlet"),
        PLAYER("Player");

        public final String name;

        RadarTargetType(String name) {
            this.name = name;
        }
    }

    RadarTargetType getTargetType();
}
