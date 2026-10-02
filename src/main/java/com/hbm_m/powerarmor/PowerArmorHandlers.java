package com.hbm_m.powerarmor;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code ModEventHandlerClient.clientTick} (Phase START): Schritthoehe der FSB-Sets
 * ({@code stepSize}) fuer den Spieler auf der Client-Seite. Die Kampf-, Sprung-, Fall- und Tick-Haken
 * der Sets laufen ueber {@code HbmForgeEvents} und {@code DamageResistanceHandler}.
 */
public final class PowerArmorHandlers {

    private PowerArmorHandlers() {}

    public static void register() {
        TickEvent.PLAYER_PRE.register(PowerArmorHandlers::onPlayerTickPre);
    }

    private static void onPlayerTickPre(Player player) {
        if (!player.level().isClientSide()) return;

        float discriminator = 0.003F;
        float defaultStepSize = 0.6F;
        int newStepSize = 0;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof ModArmorFSB plate) {
            if (ModArmorFSB.hasFSBArmor(player)) newStepSize = plate.stepSize;
        }

        if (newStepSize > 0) {
            setStep(player, newStepSize + discriminator);
        } else {
            for (int i = 1; i < 4; i++) if (getStep(player) == i + discriminator) setStep(player, defaultStepSize);
        }
    }

    private static float getStep(Player player) {
        //? if < 1.21.1 {
        return player.maxUpStep();
        //?} else {
        /*var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        return attr == null ? 0.6F : (float) attr.getBaseValue();
        *///?}
    }

    private static void setStep(Player player, float step) {
        //? if < 1.21.1 {
        player.setMaxUpStep(step);
        //?} else {
        /*var attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        if (attr != null) attr.setBaseValue(step);
        *///?}
    }
}
