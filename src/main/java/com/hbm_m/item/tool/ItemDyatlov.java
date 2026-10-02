package com.hbm_m.item.tool;

import com.hbm_m.blockentity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.blockentity.machines.rbmk.RBMKColumnBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1:1 {@code com.hbm.items.tool.ItemDyatlov} ({@code meltdown_tool}): RBMK-Kernschmelze bzw. ZIRNOX auf 200000 Hitze. */
public class ItemDyatlov extends Item {

    public ItemDyatlov(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();

        if (!world.isClientSide) {
            BlockPos pos = ctx.getClickedPos();
            BlockEntity te = world.getBlockEntity(pos);

            if (te instanceof IMultiblockPart part && part.getControllerPos() != null) {
                te = world.getBlockEntity(part.getControllerPos());
            }

            if (te instanceof RBMKColumnBlockEntity rbmk) {
                RBMKColumnBlockEntity.meltdownReactor(world, rbmk);
            }

            if (te instanceof MachineZirnoxBlockEntity zirnox) {
                zirnox.heat = 200000;
            }
        }

        return InteractionResult.PASS;
    }
}
