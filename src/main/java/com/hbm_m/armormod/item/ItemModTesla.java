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

/** 1:1 {@code ItemModTesla} (back_tesla): Blitze auf Wesen im Umkreis 5, nur mit voller Energie-Ruestung. */
public class ItemModTesla extends ItemArmorMod {

    public List<net.minecraft.world.phys.Vec3> targets = new java.util.ArrayList<>();

    public ItemModTesla() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.plate_only, false, true, false, false);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Zaps nearby entities (requires full electric set)", ChatFormatting.YELLOW));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(stack.getHoverName().copy().append(" (zaps nearby entities)").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide && entity instanceof Player player && armor.getItem() instanceof com.hbm_m.powerarmor.ModArmorFSBPowered && com.hbm_m.powerarmor.ModArmorFSB.hasFSBArmor(player)) {
            targets = com.hbm_m.blockentity.machines.TeslaBlockEntity.zap(entity.level(), entity.getX(), entity.getY() + 1.25, entity.getZ(), 5, entity);

            if (targets != null && !targets.isEmpty() && entity.getRandom().nextInt(5) == 0) {
                armor.hurtAndBreak(1, entity, e -> {});
            }
        }
    }
}
