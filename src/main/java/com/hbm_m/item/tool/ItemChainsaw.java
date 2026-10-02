package com.hbm_m.item.tool;

import java.util.function.Supplier;

import net.minecraft.world.item.Tier;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.items.tool.ItemChainsaw}: treibstoffbetriebene Axt mit Kettensaegen-Faehigkeiten.
 * Die Schwung-Animation ({@code IAnimatedItem<ToolAnimation>}, {@code SWING_ROT}/{@code SWING_TRANS}) haengt am
 * Animationssystem der Waffen ({@code HbmAnimations}/{@code BusAnimation}) und kommt mit dessen Port;
 * {@code IHeldSoundProvider} ist im Original eine leere Markierung ohne Verwendung.
 */
public class ItemChainsaw extends ItemToolAbilityFueled {

    @SafeVarargs
    public ItemChainsaw(float damage, double movement, Tier material, EnumToolType type, int maxFuel, int consumption, int fillRate, Supplier<Fluid>... acceptedFuels) {
        super(damage, movement, material, type, maxFuel, consumption, fillRate, acceptedFuels);
    }
}
