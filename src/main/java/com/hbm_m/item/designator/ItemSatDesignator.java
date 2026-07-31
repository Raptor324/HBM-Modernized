package com.hbm_m.item.designator;

import com.hbm_m.item.ISatChip;
import com.hbm_m.item.satellite.ItemSatChip;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.hbm_m.sound.ModSounds;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Remote control for an orbited satellite: sneak + right-click sets its own frequency
 * ({@link ItemSatChip}); a plain right-click raytraces 300 blocks and fires
 * {@link Satellite#onCoordAction} on whatever satellite shares that frequency (e.g. Gerald's
 * one-time orbital strike). Port of legacy {@code com.hbm.items.tool.ItemSatDesignator}.
 */
public class ItemSatDesignator extends ItemSatChip {

    public ItemSatDesignator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(level instanceof ServerLevel server)) {
            return InteractionResultHolder.pass(stack);
        }

        HitResult hit = player.pick(300, 1f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        var pos = blockHit.getBlockPos().relative(blockHit.getDirection());

        Satellite sat = SatelliteManager.get(server).getSatFromFreq(ISatChip.getFreqS(stack));
        if (sat == null) {
            return InteractionResultHolder.pass(stack);
        }

        sat.onCoordAction(server, player, pos.getX(), pos.getY(), pos.getZ());
        level.playSound(null, player.blockPosition(), ModSounds.TOOL_TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.success(stack);
    }
}
