//? if neoforge {
/*package com.hbm_m.item.tool;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/^*
 * NeoForge 21.1 hat {@code IItemExtension.onBlockStartBreak} gestrichen. Forge 1.20.1 ruft es in
 * {@code ServerPlayerGameMode.destroyBlock} direkt nach dem {@code BreakEvent} auf; {@code true} bricht den Abbau ab.
 * Diese Bruecke haengt sich als letzter Empfaenger an das {@code BreakEvent} und bildet das nach
 * (Abbruch = Event abbrechen). Waehrend des Aufrufs feuern die Werkzeuge selbst weitere BreakEvents
 * (Flaechen-/Aderabbau) - die werden nicht erneut umgeleitet.
 ^/
@EventBusSubscriber(modid = com.hbm_m.lib.RefStrings.MODID)
public final class BlockStartBreakNeoForge {
    private BlockStartBreakNeoForge() {}

    private static boolean active = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (active || event.isCanceled()) return;
        net.minecraft.world.entity.player.Player player = event.getPlayer();
        if (player == null) return;
        net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;
        active = true;
        try {
            boolean cancel = false;
            if (stack.getItem() instanceof ItemToolAbility tool) cancel = tool.onBlockStartBreak(stack, event.getPos(), player);
            else if (stack.getItem() instanceof ItemConveyorWand wand) cancel = wand.onBlockStartBreak(stack, event.getPos(), player);
            if (cancel) event.setCanceled(true);
        } finally {
            active = false;
        }
    }
}
*///?}
