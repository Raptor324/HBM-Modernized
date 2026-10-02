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

/** 1:1 {@code ItemModSensor} (gas_tester): piept bei gefaehrlichen Gasen, auch im Inventar. */
public class ItemModSensor extends ItemArmorMod {

    public ItemModSensor() {
        super(new Properties().stacksTo(1), ArmorModificationHelper.extra, true, true, true, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(line("Beeps near hazardous gasses", ChatFormatting.YELLOW));
        list.add(line("Works in the inventory or when applied to armor", ChatFormatting.YELLOW));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.YELLOW, stack, " (Detects gasses)"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (entity instanceof LivingEntity living) {
            modUpdate(living, null);
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        Level world = entity.level();
        if (world.isClientSide || world.getGameTime() % 20 != 0) return;

        int x = (int) Math.floor(entity.getX());
        int y = (int) Math.floor(entity.getEyeY());
        int z = (int) Math.floor(entity.getZ());

        boolean poison = false;
        boolean explosive = false;

        for (int i = -3; i <= 3; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -3; k <= 3; k++) {
                    net.minecraft.world.level.block.Block b = world.getBlockState(new net.minecraft.core.BlockPos(x + i * 2, y + j * 2, z + k * 2)).getBlock();

                    if (b == com.hbm_m.block.ModBlocks.GAS_ASBESTOS.get() || b == com.hbm_m.block.ModBlocks.GAS_COAL.get() || b == com.hbm_m.block.ModBlocks.GAS_RADON.get()
                            || b == com.hbm_m.block.ModBlocks.GAS_MONOXIDE.get() || b == com.hbm_m.block.ModBlocks.GAS_RADON_DENSE.get() || b == com.hbm_m.block.ModBlocks.CHLORINE_GAS.get()) {
                        poison = true;
                    }

                    if (b == com.hbm_m.block.ModBlocks.GAS_FLAMMABLE.get() || b == com.hbm_m.block.ModBlocks.GAS_EXPLOSIVE.get()) {
                        explosive = true;
                    }
                }
            }
        }

        if (explosive) {
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.follyAquired"), net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
        } else if (poison) {
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:item.techBoop"), net.minecraft.sounds.SoundSource.PLAYERS, 2F, 1.5F);
        }
    }
}
