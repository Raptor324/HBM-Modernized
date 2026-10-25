package com.hbm_m.item;

import com.hbm_m.network.HbmAnimationPacket;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.render.anim.BusAnimation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code IAnimatedItem}: Gegenstaende mit Bus-Animationen ausserhalb des SEDNA-Waffensystems (Granaten, Werkzeuge). */
public interface IAnimatedItem<T extends Enum<?>> {

    /** Fetch the animation for a given type */
    BusAnimation getAnimation(T type, ItemStack stack);

    /** Should a player holding this item aim it like a gun/bow? */
    boolean shouldPlayerModelAim(ItemStack stack);

    // Runtime erasure means we have to explicitly give the class a second time :(
    Class<T> getEnum();

    // Run a specified animation
    default void playAnimation(Player player, T type) {
        if (player instanceof ServerPlayer sp) {
            ModPacketHandler.sendToPlayer(sp, ModPacketHandler.HBM_ANIMATION, new HbmAnimationPacket(type.ordinal(), 0, 0));
        }
    }
}
