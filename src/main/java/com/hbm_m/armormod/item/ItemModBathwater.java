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

/** 1:1 {@code ItemModBathwater} (bathwater / bathwater_mk2): vergiftet Angreifer. */
public class ItemModBathwater extends ItemArmorMod {

    public ItemModBathwater() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    private ChatFormatting color() {
        if (this == ModItems.BATHWATER_MK2.get()) return blink(ChatFormatting.GREEN, ChatFormatting.YELLOW);
        return blink(ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Inflicts poison on the attacker", color()));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(color(), stack, " (Poisons attackers)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        if (!entity.level().isClientSide) {
            if (event.source.getEntity() instanceof LivingEntity attacker) {
                if (this == ModItems.BATHWATER.get())
                    attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
                if (this == ModItems.BATHWATER_MK2.get())
                    attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 4));
            }
        }
    }
}
