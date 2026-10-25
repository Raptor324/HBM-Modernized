package com.hbm_m.item.designator;

import com.hbm_m.item.satellite.ItemSatChip;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code com.hbm.items.tool.ItemSatDesignator}: Rechtsklick zielt 300 Bloecke weit und loest beim Satelliten
 * mit gleicher Frequenz {@link Satellite#onCoordAction} am Block vor der getroffenen Seite aus. Die Frequenz setzt
 * wie bei allen Chips der Satelliten-Verknuepfer.
 */
public class ItemSatDesignator extends ItemSatChip {

    public ItemSatDesignator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world instanceof ServerLevel server) {
            Satellite sat = SatelliteManager.get(server).getSatFromFreq(this.getFreq(stack));

            if (sat != null) {
                HitResult hit = player.pick(300, 1F, false);

                if (hit instanceof BlockHitResult pos && hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos target = pos.getBlockPos().relative(pos.getDirection());
                    sat.onCoordAction(server, player, target.getX(), target.getY(), target.getZ());
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }
}
