package com.hbm_m.item.weapon.grenade;

import net.minecraft.world.item.Item;

/** 1:1 {@code ItemGrenadeShell}: jede Original-Meta ist ein eigener Gegenstand {@code grenade_shell_<typ>}. */
public class ItemGrenadeShell extends Item {

    /*
     *  __________
     * |          | ________ SHELL COLOR - Changes based on filling
     * |   _____  |
     * |  |    _____________ LABEL COLOR - Changes based on filling
     * |  |_____| |
     *  \________/__________ FUZE INDICATOR RING - Changes based on installed fuze
     *   \______/
     *    |    |
     *    |    |
     *    |   ______________ MISC - Remains static for this shell
     *    |    |
     *    /    \
     *   |______|
     *     {__}
     */

    public final EnumGrenadeShell type;

    public ItemGrenadeShell(EnumGrenadeShell type, Properties props) {
        super(props);
        this.type = type;
    }

    public static enum EnumGrenadeShell {
        FRAG(4, 30, 0.5D, 1D),      // bonus fragmentation
        STICK(4, 43, 0.25D, 1.5D),  // thrown farther
        TECH(2, 30, 0.5D, 1D),      // casing with electronics for EMP/plasma
        NUKE(1, 43, 0.25D, 1.5D);   // nuka grenade casing for high yield grenades

        private int stackLimit;
        private int drawDuration;
        private double bounceModifier = 1D;
        private double yeetForce = 1D;

        private EnumGrenadeShell(int stackLimit, int drawDuration, double bounceModifier, double yeetForce) {
            this.stackLimit = stackLimit;
            this.drawDuration = drawDuration;
            this.bounceModifier = bounceModifier;
            this.yeetForce = yeetForce;
        }

        public int getStackLimit() { return stackLimit; }
        public int getDrawDuration() { return drawDuration; }
        public double getBounce() { return bounceModifier; }
        public double getYeetForce() { return yeetForce; }
    }
}
