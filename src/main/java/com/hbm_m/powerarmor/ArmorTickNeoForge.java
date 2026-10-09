//? if neoforge {
/*package com.hbm_m.powerarmor;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/^*
 * NeoForge 21.1 hat {@code IItemExtension.onArmorTick} gestrichen. Forge 1.20.1 ruft es jeden Tick fuer jedes
 * getragene Ruestungsteil des Spielers auf (Client und Server, Reihenfolge Stiefel -> Helm). Diese Bruecke ruft
 * die Port-Methoden {@code onArmorTick} der Ruestungen in derselben Reihenfolge auf.
 ^/
@EventBusSubscriber(modid = com.hbm_m.lib.RefStrings.MODID)
public final class ArmorTickNeoForge {
    private ArmorTickNeoForge() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        net.minecraft.world.level.Level level = player.level();
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().armor) {
            if (stack.isEmpty()) continue;
            net.minecraft.world.item.Item item = stack.getItem();
            if (item instanceof ModArmorFSB fsb) fsb.onArmorTick(stack, level, player);
            else if (item instanceof ArmorNo9 no9) no9.onArmorTick(stack, level, player);
            else if (item instanceof ArmorEuphemium eu) eu.onArmorTick(stack, level, player);
            else if (item instanceof com.hbm_m.armormod.item.JetpackBase jet) jet.onArmorTick(stack, level, player);
        }
    }
}
*///?}
