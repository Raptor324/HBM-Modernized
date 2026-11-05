package com.hbm_m.item.tool;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntityKa52;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Stellt einen Ka-52 auf den angeklickten Block. Die Nase zeigt in Blickrichtung des Spielers. */
public class ItemKa52 extends Item {

    public ItemKa52(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityKa52 helicopter = ModEntities.KA52.get().create(level);
        if (helicopter == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0.0F;
        helicopter.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        if (player != null) {
            helicopter.setCaller(player.getUUID());
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        level.addFreshEntity(helicopter);
        return InteractionResult.CONSUME;
    }
}
