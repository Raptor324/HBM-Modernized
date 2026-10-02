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

/** 1:1 {@code ItemModLodestone} (lodestone, horseshoe_magnet, industrial_magnet): zieht Items an. */
public class ItemModLodestone extends ItemArmorMod {

    int range;

    public ItemModLodestone(int range) {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
        this.range = range;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Attracts nearby items", ChatFormatting.DARK_GRAY));
        list.add(line("Item attraction range: " + range, ChatFormatting.DARK_GRAY));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.DARK_GRAY, stack, " (Magnetic range: " + range + ")"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        // No magnet if keybind toggled
        if (entity instanceof Player p && !HbmPlayerProps.getData(p).isMagnetActive()) return;

        List<net.minecraft.world.entity.item.ItemEntity> items = entity.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, entity.getBoundingBox().inflate(range, range, range));

        for (net.minecraft.world.entity.item.ItemEntity item : items) {
            net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(entity.getX() - item.getX(), entity.getY() - item.getY(), entity.getZ() - item.getZ()).normalize();
            net.minecraft.world.phys.Vec3 m = item.getDeltaMovement().add(vec.x * 0.05, vec.y * 0.05, vec.z * 0.05);
            if (vec.y > 0 && m.y < 0.04)
                m = m.add(0, 0.2, 0);
            item.setDeltaMovement(m);
        }
    }
}
