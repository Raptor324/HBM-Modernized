package com.hbm_m.item.weapon.sedna.hud;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code HUDComponentDurabilityBar}: Abnutzungsbalken ueber dem Munitionszaehler. */
public class HUDComponentDurabilityBar implements IHUDComponent {

    public boolean mirrored = false;

    public HUDComponentDurabilityBar() {
        this(false);
    }

    public HUDComponentDurabilityBar(boolean mirror) {
        this.mirrored = mirror;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 5;
    }

    @Override
    public void renderHUDComponent(Object gui, Player player, ItemStack stack, int bottomOffset, int gunIndex) {
        com.hbm_m.client.weapon.GunHud.durabilityBar(this, gui, player, stack, bottomOffset, gunIndex);
    }
}
