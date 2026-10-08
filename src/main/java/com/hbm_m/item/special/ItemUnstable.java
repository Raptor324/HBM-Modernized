package com.hbm_m.item.special;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemUnstable} ({@code ingot_u238m2}, Meta 0): zaehlt im Inventar jeden Tick
 * {@code timer} hoch und zuendet bei Erreichen eine MK5-Atomexplosion mit {@code radius}; der Traeger bekommt
 * 10000 Atomschaden und der Stapel verschwindet. Die Bruchstuecke (Meta 1-3) sind im Port eigene, inerte Gegenstaende.
 */
public class ItemUnstable extends Item {

    final int radius;
    final int timer;

    public ItemUnstable(int radius, int timer, Properties properties) {
        super(properties);
        this.radius = radius;
        this.timer = timer;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Decay: " + (getTimer(stack) * 100 / timer) + "%"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int i, boolean b) {

        this.setTimer(stack, this.getTimer(stack) + 1);

        if (this.getTimer(stack) == timer && !world.isClientSide) {
            com.hbm_m.entity.logic.EntityNukeExplosionMK5.start(world, radius, entity.getX(), entity.getY(), entity.getZ());
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:entity.oldExplosion"), SoundSource.PLAYERS, 1.0F, 1.0F);
            entity.hurt(com.hbm_m.damagesource.ModDamageSources.nuclearBlast(world), 10000);

            stack.setCount(0);
        }
    }

    private void setTimer(ItemStack stack, int time) {
        StackNbt.orCreate(stack).putInt("timer", time);
    }

    private int getTimer(ItemStack stack) {
        CompoundTag tag = StackNbt.read(stack);
        return tag == null ? 0 : tag.getInt("timer");
    }
}
