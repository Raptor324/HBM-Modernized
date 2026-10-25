package com.hbm_m.item.weapon.sedna.hud;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code HUDComponentAmmoCounter}: Munitionssymbol und "x / y" rechts neben der Hotbar. */
public class HUDComponentAmmoCounter implements IHUDComponent {

    public final int receiver;
    public boolean mirrored;
    public boolean noCounter;

    public HUDComponentAmmoCounter(int receiver) {
        this.receiver = receiver;
    }

    public HUDComponentAmmoCounter mirror() {
        this.mirrored = true;
        return this;
    }

    public HUDComponentAmmoCounter noCounter() {
        this.noCounter = true;
        return this;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 17;
    }

    @Override
    public void renderHUDComponent(Object gui, Player player, ItemStack stack, int bottomOffset, int gunIndex) {
        com.hbm_m.client.weapon.GunHud.ammoCounter(this, gui, player, stack, bottomOffset, gunIndex);
    }
}
