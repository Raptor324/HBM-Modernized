package com.hbm_m.armormod.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemModPads}: weniger Fallschaden, statische Pads laden Energie-Ruestung beim Gehen. */
public class ItemModPads extends ItemArmorMod {

    float damageMod;

    public ItemModPads(float damageMod) {
        super(new Properties().stacksTo(1), ArmorModificationHelper.boots_only, false, false, false, true);
        this.damageMod = damageMod;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        if (damageMod != 1F)
            list.add(line("-" + Math.round((1F - damageMod) * 100) + "% fall damage", ChatFormatting.RED));
        if (this == ModItems.PADS_STATIC.get())
            list.add(line("Passively charges electric armor when walking", ChatFormatting.DARK_PURPLE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        if (this == ModItems.PADS_STATIC.get())
            list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (-" + Math.round((1F - damageMod) * 100) + "% fall dmg / passive charge)"));
        else
            list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (-" + Math.round((1F - damageMod) * 100) + "% fall dmg)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (event.source.is(net.minecraft.world.damagesource.DamageTypes.FALL))
            event.amount *= damageMod;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide && this == ModItems.PADS_STATIC.get() && entity instanceof Player player) {
            if (player.walkDist != player.walkDistO) {
                if (com.hbm_m.powerarmor.ModArmorFSB.hasFSBArmorIgnoreCharge(player)) {
                    for (ItemStack stack : player.getInventory().armor) {
                        if (!stack.isEmpty() && stack.getItem() instanceof com.hbm_m.powerarmor.ModArmorFSBPowered powered) {
                            long charge = powered.drain / 2;
                            if (charge == 0)
                                charge = powered.consumption / 40;
                            long power = Math.min(powered.getMaxCharge(stack), powered.getCharge(stack) + charge);
                            powered.setCharge(stack, power);
                        }
                    }
                }
            }
        }
    }
}
