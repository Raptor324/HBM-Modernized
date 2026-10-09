package com.hbm_m.item.weapon.sedna.impl;

import com.hbm_m.platform.StackNbt;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.mods.XWeaponModManager;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemGunNI4NI}: laedt alle 80 Ticks eine Muenze nach (max. 4, +2 je Nickel-/Dublonen-Mod) und laesst sich
 * per {@link #customize} ({@code /ntmcustomize}) einfaerben.
 */
public class ItemGunNI4NI extends ItemGunBaseNT implements com.hbm_m.item.ICustomizable {

    public ItemGunNI4NI(WeaponQuality quality, GunConfig... cfg) {
        super(quality, cfg);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {
        super.inventoryTick(stack, world, entity, slot, isHeld);

        if (!world.isClientSide) {

            int maxCoin = 4;
            if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_NICKEL)) maxCoin += 2;
            if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_DOUBLOONS)) maxCoin += 2;

            if (getCoinCount(stack) < maxCoin) {
                setCoinCharge(stack, getCoinCharge(stack) + 1);

                if (getCoinCharge(stack) >= 80) {
                    setCoinCharge(stack, 0);
                    int newCount = getCoinCount(stack) + 1;
                    setCoinCount(stack, newCount);

                    if (isHeld) {
                        SoundEvent ev = HbmSoundsNT.get("hbm:item.techBoop");
                        if (ev != null) world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ev, SoundSource.PLAYERS, 1.0F, 1F + newCount / (float) maxCoin);
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
        Level level = com.hbm_m.platform.PlatformHooks.tooltipLevel(hbmTooltipCtx);
    *///?}
        list.add(Component.literal("Now, don't get the wrong idea."));
        list.add(Component.literal("I ").append(Component.literal("fucking hate ").withStyle(ChatFormatting.RED)).append(Component.literal("this game.").withStyle(ChatFormatting.GRAY)));
        list.add(Component.literal("I didn't do this for you, I did it for sea."));
        //? if < 1.21.1 {
        super.appendHoverText(stack, level, list, flag);
        //?} else {
        /*super.appendHoverText(stack, hbmTooltipCtx, list, flag);
        *///?}
    }

    @Override
    public void customize(Player player, ItemStack stack, String... args) {

        if (args.length == 0) {
            resetColors(stack);
            player.sendSystemMessage(Component.literal("Colors reset!").withStyle(ChatFormatting.GREEN));
            return;
        }

        if (args.length != 3) {
            resetColors(stack);
            player.sendSystemMessage(Component.literal("Requires three hexadecimal colors!").withStyle(ChatFormatting.RED));
            return;
        }

        try {
            int dark = Integer.parseInt(args[0], 16);
            int light = Integer.parseInt(args[1], 16);
            int grip = Integer.parseInt(args[2], 16);

            if (dark < 0 || dark > 0xffffff || light < 0 || light > 0xffffff || grip < 0 || grip > 0xffffff) {
                player.sendSystemMessage(Component.literal("Colors must range from 0 to FFFFFF!").withStyle(ChatFormatting.RED));
                return;
            }

            setColors(stack, dark, light, grip);
            player.sendSystemMessage(Component.literal("Colors set!").withStyle(ChatFormatting.GREEN));

        } catch (Throwable ex) {
            player.sendSystemMessage(Component.literal(String.valueOf(ex.getLocalizedMessage())).withStyle(ChatFormatting.RED));
        }
    }

    public static void resetColors(ItemStack stack) {
        if (!StackNbt.has(stack)) return;
        StackNbt.tag(stack).remove("colors");
    }

    public static void setColors(ItemStack stack, int dark, int light, int grip) {
        CompoundTag tag = StackNbt.orCreate(stack);
        tag.putIntArray("colors", new int[] {dark, light, grip});
    }

    @Nullable
    public static int[] getColors(ItemStack stack) {
        if (!StackNbt.has(stack) || !StackNbt.read(stack).contains("colors")) return null;
        int[] colors = StackNbt.read(stack).getIntArray("colors");
        if (colors.length != 3) return null;
        return colors;
    }

    public static final String KEY_COIN_COUNT = "coincount";
    public static final String KEY_COIN_CHARGE = "coincharge";
    public static int getCoinCount(ItemStack stack) { return getValueInt(stack, KEY_COIN_COUNT); }
    public static void setCoinCount(ItemStack stack, int value) { setValueInt(stack, KEY_COIN_COUNT, value); }
    public static int getCoinCharge(ItemStack stack) { return getValueInt(stack, KEY_COIN_CHARGE); }
    public static void setCoinCharge(ItemStack stack, int value) { setValueInt(stack, KEY_COIN_CHARGE, value); }
}
