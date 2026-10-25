package com.hbm_m.blockentity.deco;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.effect.EntityModFX;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntityVent}: jeden Tick mit Strom eine Wolke an einer gaussverteilten Stelle, die kein Vollblock ist. */
public class VentBlockEntity extends BlockEntity {

    public VentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENT.get(), pos, state);
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, VentBlockEntity te) {
        if (!world.hasNeighborSignal(pos)) return;
        RandomSource rand = world.random;
        double spread;
        int type;
        if (state.is(ModBlocks.VENT_CHLORINE.get())) { spread = 1.5; type = 0; }
        else if (state.is(ModBlocks.VENT_CLOUD.get())) { spread = 1.75; type = 1; }
        else if (state.is(ModBlocks.VENT_PINK_CLOUD.get())) { spread = 2; type = 2; }
        else return;

        double x = rand.nextGaussian() * spread;
        double y = rand.nextGaussian() * spread;
        double z = rand.nextGaussian() * spread;
        BlockPos p = new BlockPos(pos.getX() + (int) x, pos.getY() + (int) y, pos.getZ() + (int) z);
        BlockState b = world.getBlockState(p);
        if (b.isSolidRender(world, p) && !b.isSignalSource()) return;

        EntityModFX fx = switch (type) {
            case 0 -> new EntityModFX.Chlorine(world, p.getX(), p.getY(), p.getZ(), x / 2, y / 2, z / 2);
            case 1 -> new EntityModFX.Cloud(world, p.getX(), p.getY(), p.getZ(), x / 2, y / 2, z / 2);
            default -> new EntityModFX.PinkCloud(world, p.getX(), p.getY(), p.getZ(), x / 2, y / 2, z / 2);
        };
        world.addFreshEntity(fx);
    }
}
