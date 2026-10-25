package com.hbm_m.handler.ability;

import java.util.function.Function;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Beute der Waffenfaehigkeit {@link IWeaponAbility#BOBBLE}: im Original
 * {@code new ItemStack(ModBlocks.bobblehead, 1, rand.nextInt(BobbleType.values().length - 1) + 1)}.
 * Der Wackelkopf-Block ({@code BlockBobble} mit Tile-Entity, Renderer und GUI) ist noch nicht portiert;
 * sobald er existiert, traegt er hier seine Zufallsauswahl ein.
 */
public final class BobbleDrop {

    private BobbleDrop() {}

    private static Function<Level, ItemStack> factory = level -> com.hbm_m.item.TrinketBlockItem.make(
            com.hbm_m.block.ModBlocks.BOBBLEHEAD.get().asItem(),
            level.getRandom().nextInt(com.hbm_m.block.decorations.TrinketTypes.BobbleType.values().length - 1) + 1);

    public static void setFactory(Function<Level, ItemStack> f) {
        factory = f;
    }

    public static ItemStack randomBobblehead(Level level) {
        return factory.apply(level);
    }
}
