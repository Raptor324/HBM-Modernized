package com.hbm_m.render.anim;

import com.hbm_m.client.weapon.GunGL;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code HbmAnimations} (nur Client): Keyframe-Animationen je Hotbar-Platz und Waffen-Konfiguration (9 x 8 Schienen).
 * Eine Animation gilt nur, solange im selben Platz derselbe Gegenstand liegt.
 */
public class HbmAnimations {

    //in flans mod and afaik also MW, there's an issue that there is only one
    //single animation timer for each client. this is fine for the most part,
    //but once you reload and switch weapons while the animation plays, the
    //other weapon will too play the animation, even though it is not reloading.
    //my approach adds 9 timers, one for every inventory slot. you can still
    //"trick" the system by putting a weapon into a different slot while an
    //animation is playing, though this will cancel the animation entirely.
    public static final Animation[][] hotbar = new Animation[9][8]; //now with 8 parallel rails per slot! time to get railed!

    /** Keyframe-based animation system primarily used for guns, not to be confused with Drillgon's animloader system. */
    public static class Animation {

        //the "name" of the animation slot. if the item has a different key than
        //the animation, the animation will be canceled.
        public String key;
        //the starting time of the animation
        public long startMillis;
        //the animation bus
        public BusAnimation animation;
        // If set, don't cancel this animation when the timer ends, instead wait for the next to start
        public boolean holdLastFrame = false;

        public Animation(String key, long startMillis, BusAnimation animation) {
            this.key = key;
            this.startMillis = startMillis;
            this.animation = animation;
        }

        public Animation(String key, long startMillis, BusAnimation animation, boolean holdLastFrame) {
            this(key, startMillis, animation);
            this.holdLastFrame = holdLastFrame;
        }
    }

    public static String keyOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static Animation getRelevantAnim() { return getRelevantAnim(0); }

    public static Animation getRelevantAnim(int index) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return null;
        int slot = player.getInventory().selected;
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) return null;

        if (slot < 0 || slot > 8) { //for freak of nature hotbars, probably won't work right but at least it doesn't crash
            slot = Math.abs(slot) % 9;
        }

        if (hotbar[slot][index] == null) return null;

        if (hotbar[slot][index].key.equals(keyOf(stack))) {
            return hotbar[slot][index];
        }

        return null;
    }

    public static double[] getRelevantTransformation(String bus) { return getRelevantTransformation(bus, 0); }

    public static double[] getRelevantTransformation(String bus, int index) {

        Animation anim = HbmAnimations.getRelevantAnim(index);

        if (anim != null) {
            BusAnimation buses = anim.animation;
            int millis = (int) (System.currentTimeMillis() - anim.startMillis);

            BusAnimationSequence seq = buses.getBus(bus);

            if (seq != null) {
                double[] trans = seq.getTransformation(millis);
                if (trans != null) return trans;
            }
        }

        return new double[] {
                0, 0, 0, // position
                0, 0, 0, // rotation
                1, 1, 1, // scale
                0, 0, 0, // offset
                0, 1, 2, // XYZ order
        };
    }

    public static void applyRelevantTransformation(String bus) { applyRelevantTransformation(bus, 0); }

    public static void applyRelevantTransformation(String bus, int index) {
        double[] transform = getRelevantTransformation(bus, index);
        int[] rot = new int[] { (int) transform[12], (int) transform[13], (int) transform[14] };

        GunGL.translate(transform[0], transform[1], transform[2]);
        GunGL.rotate(transform[3 + rot[0]], rot[0] == 0 ? 1 : 0, rot[0] == 1 ? 1 : 0, rot[0] == 2 ? 1 : 0);
        GunGL.rotate(transform[3 + rot[1]], rot[1] == 0 ? 1 : 0, rot[1] == 1 ? 1 : 0, rot[1] == 2 ? 1 : 0);
        GunGL.rotate(transform[3 + rot[2]], rot[2] == 0 ? 1 : 0, rot[2] == 1 ? 1 : 0, rot[2] == 2 ? 1 : 0);
        GunGL.translate(-transform[9], -transform[10], -transform[11]);
        GunGL.scale(transform[6], transform[7], transform[8]);
    }
}
