package com.hbm_m.item.tool;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityPantsir;
import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Stellt den fahrbaren Pantsir-S1 auf den angeklickten Block, die Kabine zeigt in Blickrichtung. */
public class ItemPantsir extends MultiblockBlockItem {

    public ItemPantsir(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = new BlockPlaceContext(context).getClickedPos();
        EntityPantsir vehicle = ModEntities.PANTSIR.get().create(level);
        if (vehicle == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        vehicle.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player != null ? player.getYRot() : 0.0F, 0.0F);
        if (player != null) {
            vehicle.setOwner(player.getUUID());
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        level.addFreshEntity(vehicle);
        return InteractionResult.CONSUME;
    }
}
