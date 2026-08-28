package com.hbm_m.item.rbmk;

import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RBMKLidItem extends Item {

    /** 1 = concrete lid, 2 = glass lid */
    public final int lidType;

    public RBMKLidItem(int lidType, Properties props) {
        super(props);
        this.lidType = lidType;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        // CE resolves the clicked block through RBMKBase.findCore, so clicking anywhere on the
        // column - including its dummy segments, which is where the lid visually goes - works. The
        // port only accepted a click on the base block, which is normally buried in the floor.
        com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity col =
                com.hbm_m.blockentity.machines.rbmk.RBMKSteamInletBlockEntity.findColumnCore(level, pos);
        if (col != null && col.isLidRemovable() && !col.hasLid()) {
            col.setLidState(lidType);
            if (!ctx.getPlayer().isCreative())
                ctx.getItemInHand().shrink(1);
            // Play appropriate placement sound
            if (lidType == 2)
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            else
                level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
