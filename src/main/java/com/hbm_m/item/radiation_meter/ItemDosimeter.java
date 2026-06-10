package com.hbm_m.item.radiation_meter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Порт {@link com.hbm.items.tool.ItemDosimeter} (1.7.10).
 */
public class ItemDosimeter extends Item {

    private final Random rand = new Random();

    public ItemDosimeter(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof LivingEntity) || world.isClientSide()) {
            return;
        }

        float x = HbmLivingProps.getRadBuf((LivingEntity) entity);

        if (world.getGameTime() % 5 == 0) {
            if (x > 1E-5F) {
                List<Integer> list = new ArrayList<>();

                if (x < 0.5F) {
                    list.add(0);
                }
                if (x < 1F) {
                    list.add(1);
                }
                if (x >= 0.5F && x < 2F) {
                    list.add(2);
                }
                if (x >= 1F && x >= 2F) {
                    list.add(3);
                }

                int r = list.get(rand.nextInt(list.size()));

                if (r > 0) {
                    playGeigerSound(world, entity, r);
                }
            } else if (rand.nextInt(100) == 0) {
                playGeigerSound(world, entity, 1);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            ModSounds.TOOL_TECH_BOOP.ifPresent(sound ->
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));
            ContaminationUtil.printDosimeterData(player);
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }

    private static void playGeigerSound(Level world, Entity entity, int index) {
        SoundEvent sound = switch (index) {
            case 1 -> ModSounds.GEIGER_1.orElse(null);
            case 2 -> ModSounds.GEIGER_2.orElse(null);
            case 3 -> ModSounds.GEIGER_3.orElse(null);
            default -> null;
        };
        if (sound != null) {
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
