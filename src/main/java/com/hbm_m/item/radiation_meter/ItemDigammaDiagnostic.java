package com.hbm_m.item.radiation_meter;

import com.hbm_m.sound.ModSounds;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Порт {@link com.hbm.items.tool.ItemDigammaDiagnostic} (1.7.10).
 */
public class ItemDigammaDiagnostic extends Item {

    public ItemDigammaDiagnostic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            ModSounds.TOOL_TECH_BOOP.ifPresent(sound ->
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F));
            ContaminationUtil.printDiagnosticData(player);
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }
}
