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

/** 1:1 {@code ItemModInk}: 10% Schaden aufheben, Blumen. */
public class ItemModInk extends ItemArmorMod {

    private static final net.minecraft.world.level.block.Block[] RED_FLOWER = {
            net.minecraft.world.level.block.Blocks.POPPY, net.minecraft.world.level.block.Blocks.BLUE_ORCHID,
            net.minecraft.world.level.block.Blocks.ALLIUM, net.minecraft.world.level.block.Blocks.AZURE_BLUET,
            net.minecraft.world.level.block.Blocks.RED_TULIP, net.minecraft.world.level.block.Blocks.ORANGE_TULIP,
            net.minecraft.world.level.block.Blocks.WHITE_TULIP, net.minecraft.world.level.block.Blocks.PINK_TULIP,
            net.minecraft.world.level.block.Blocks.OXEYE_DAISY
    };

    public ItemModInk() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("10% chance to nullify damage", ChatFormatting.LIGHT_PURPLE));
        list.add(line("Flowers!", ChatFormatting.LIGHT_PURPLE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.LIGHT_PURPLE, stack, " (10% chance to nullify damage)"));
    }

    @Override
    public void modDamage(LivingEntity entity, Hurt event, ItemStack armor) {
        var rand = entity.level().random;
        if (rand.nextInt(10) == 0) {
            event.amount = 0;
            if (!entity.level().isClientSide) {
                if (rand.nextInt(10) == 0)
                    entity.spawnAtLocation(new ItemStack(net.minecraft.world.level.block.Blocks.DANDELION), 1.0F);
                entity.spawnAtLocation(new ItemStack(RED_FLOWER[rand.nextInt(9)]), 1.0F);
            }
        }
    }
}
