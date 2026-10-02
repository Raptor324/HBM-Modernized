package com.hbm_m.powerarmor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.tools_and_armor.ModArmorMaterials;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.armor.ArmorHat} ({@code nossy_hat}): +2 DT, zerfaellt als fallengelassenes Item. */
public class ArmorHat extends ArmorModel implements IAttackHandler, IDamageHandler, ITooltipProvider {

    public ArmorHat(ModArmorMaterials material, Type type, Properties properties) {
        super(material, type, properties);
    }

    //? if !fabric {
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        entityItem.discard();
        return true;
    }
    //?}

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("+2 DT").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void handleDamage(LivingEntity entity, ItemArmorMod.Hurt event, ItemStack stack) {

        if (DamageResistanceHandler.isUnblockable(event.source))
            return;

        event.amount -= 2F;

        if (event.amount < 0)
            event.amount = 0;
    }

    @Override
    public void handleAttack(LivingEntity entity, ItemArmorMod.Hurt event, ItemStack armor) {

        if (DamageResistanceHandler.isUnblockable(event.source))
            return;

        if (event.amount <= 2F) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 5F, 1.0F + entity.getRandom().nextFloat() * 0.5F);
            event.canceled = true;
        }
    }
}
