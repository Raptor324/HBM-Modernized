package com.hbm_m.item.fekal_electric;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemPotatos} ({@code battery_potatos}, PotatOS): Batterie (500k HE, entlaedt 100/t),
 * die in der Hand alle 10-15 Sekunden etwas sagt - je voller, desto hoeher die Stimme.
 */
public class ItemPotatos extends ModBatteryItem {

    public ItemPotatos(Properties properties, long dura, long chargeRate, long dischargeRate) {
        super(properties, dura, chargeRate, dischargeRate);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (getEnergy(stack) == 0)
            return;

        if (getTimer(stack) > 0) {
            setTimer(stack, getTimer(stack) - 1);
        } else {
            if (entity instanceof Player p) {
                if (p.getMainHandItem() == stack) {
                    float pitch = (float) getEnergy(stack) / (float) this.getCapacity() * 0.5F + 0.5F;
                    if (!world.isClientSide)
                        world.playSound(null, p.getX(), p.getY(), p.getZ(), HbmSoundsNT.get("hbm:potatos.random"), SoundSource.PLAYERS, 1.0F, pitch);
                    setTimer(stack, 200 + world.random.nextInt(100));
                }
            }
        }
    }

    private static int getTimer(ItemStack stack) {
        return StackNbt.has(stack) ? StackNbt.read(stack).getInt("timer") : 0;
    }

    private static void setTimer(ItemStack stack, int i) {
        StackNbt.orCreate(stack).putInt("timer", i);
    }
}
