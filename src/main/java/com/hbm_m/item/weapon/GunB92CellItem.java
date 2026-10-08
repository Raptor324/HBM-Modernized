package com.hbm_m.item.weapon;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code GunB92Cell} (gun_b92_ammo): zieht im Inventar je Tick eine Ladung aus einer B92 (solange diese &gt; 1 hat),
 * bis 25 Ladungen gespeichert sind. Original setzt bei 25 Meta 1 ("voll"); hier NBT "full".
 */
public class GunB92CellItem extends Item {

    public GunB92CellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (entity instanceof Player player && getPower(stack) < 25) {
            for (int j = 0; j < player.getInventory().items.size(); j++) {
                ItemStack other = player.getInventory().items.get(j);
                if (!other.isEmpty() && other.getItem() == ModItems.GUN_B92.get()) {
                    int p = GunB92Item.getPower(other);
                    if (p > 1) {
                        GunB92Item.setPower(other, p - 1);
                        setPower(stack, getPower(stack) + 1);
                        if (getPower(stack) == 25) StackNbt.orCreate(stack).putBoolean("full", true);
                        return;
                    }
                }
            }
        }
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Draws energy from the B92, allowing you to"));
        list.add(Component.literal("reload it an additional 25 times."));
        list.add(Component.literal("The cell will permanently hold its charge,"));
        list.add(Component.literal("it is not meant to be used as a battery enhancement"));
        list.add(Component.literal("for the B92, but rather as a bomb."));
        list.add(Component.literal(""));
        list.add(Component.literal("Charges: " + getPower(stack) + " / 25"));
    }

    public static int getPower(ItemStack stack) {
        return StackNbt.read(stack) == null ? 0 : StackNbt.read(stack).getInt("energy");
    }

    public static void setPower(ItemStack stack, int i) {
        StackNbt.orCreate(stack).putInt("energy", i);
    }

    /** Original getFullCell(): Meta 1, 25 Ladungen. */
    public static ItemStack getFullCell() {
        ItemStack stack = new ItemStack(ModItems.GUN_B92_AMMO.get());
        setPower(stack, 25);
        StackNbt.orCreate(stack).putBoolean("full", true);
        return stack;
    }
}
