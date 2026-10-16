package com.hbm_m.item.tool;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.item.ModItems;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of the shimmer branch of {@code WeaponSpecial}.
 *
 * <p>The two "fiend" advancements are a costume check, not a kill count: the original's
 * {@code ArmorUtil.checkForFiend} wants the jackt on your chest <em>and</em> the matching shimmer
 * weapon in hand, and it tests that every tick the weapon is in your inventory. The sledge pairs
 * with {@code jackt}, the axe with {@code jackt2}.</p>
 */
public class ItemShimmerWeapon extends Item {

    private final boolean axe;

    public ItemShimmerWeapon(boolean axe, Properties properties) {
        super(properties);
        this.axe = axe;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity,
                              int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        // checkForHeld: the original only counts the weapon actually in hand, not merely carried.
        if (!player.getMainHandItem().is(this) && !player.getOffhandItem().is(this)) return;

        Item jacket = axe ? ModItems.JACKT2.get() : ModItems.JACKT.get();
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(jacket)) return;

        ModAdvancements.grant(player, axe ? ModAdvancements.FIEND2 : ModAdvancements.FIEND);
    }
}
