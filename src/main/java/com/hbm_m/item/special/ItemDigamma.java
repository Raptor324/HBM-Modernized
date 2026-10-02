package com.hbm_m.item.special;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.QuasarEntity;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.special.ItemDigamma} ({@code particle_digamma}, Behaelter particle_empty). */
public class ItemDigamma extends Item implements ITooltipProvider {

    final int digamma;

    public ItemDigamma(int digamma, Properties properties) {
        super(properties);
        //obacht! the particle's digamma value is "ticks until half life" while the superclass' interpretation is "simply add flat value"
        this.digamma = digamma;
    }

    //? if forge {
    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return new ItemStack(ModItems.PARTICLE_EMPTY.get());
    }
    //?}

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (entity instanceof Player player) {
            ContaminationUtil.applyDigammaData(player, 1F / ((float) digamma));
        }
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("trait.hlParticle", "1.67*10³⁴a").withStyle(ChatFormatting.GOLD));
        list.add(Component.translatable("trait.hlPlayer", (digamma / 20F) + "s").withStyle(ChatFormatting.RED));
        list.add(Component.literal(""));
        float d = ((int) ((1000F / digamma) * 200F)) / 10F;
        list.add(Component.literal("[").append(Component.translatable("trait.digamma")).append("]").withStyle(ChatFormatting.RED));
        list.add(Component.literal(d + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        list.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]").withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        if (entityItem.onGround() && !entityItem.level().isClientSide) {
            if (ModClothConfig.get().dropSingularity) {
                QuasarEntity bl = new QuasarEntity(ModEntities.DIGAMMA_QUASAR.get(), entityItem.level());
                bl.setSize(5F);
                bl.setPos(entityItem.getX(), entityItem.getY(), entityItem.getZ());
                entityItem.level().addFreshEntity(bl);
            }
            entityItem.discard();
            return true;
        }
        return false;
    }
}
