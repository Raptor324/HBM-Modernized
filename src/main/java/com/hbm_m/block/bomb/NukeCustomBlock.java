package com.hbm_m.block.bomb;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.EntityFallingNuke;
import com.hbm_m.explosion.CustomNukeExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 1:1 {@code NukeCustom}: die Baukastenbombe. Zuendung (Redstone/Zuender) leert die Slots, zerstoert den Block und
 * sprengt nach {@link CustomNukeExplosion#explodeCustom}; mit Fallschirmbaugruppe wird stattdessen eine
 * {@link EntityFallingNuke} mit denselben Werten abgeworfen.
 */
public class NukeCustomBlock extends NukeBaseBlock implements IBomb {

    public NukeCustomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeCustomBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.NUKE_CUSTOM_BE.get() ? (l, p, s, be) -> NukeCustomBlockEntity.tick(l, p, s, (NukeCustomBlockEntity) be) : null;
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        // nur ueber explode(Level, BlockPos)
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {

        if (!world.isClientSide) {
            if (!(world.getBlockEntity(pos) instanceof NukeCustomBlockEntity entity)) return BombReturnCode.UNDEFINED;
            entity.update();
            CustomNukeExplosion.Values v = entity.values();

            if (!entity.isFalling()) {

                entity.clearSlots();
                world.destroyBlock(pos, false);
                CustomNukeExplosion.explodeCustom(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, v);
                return BombReturnCode.DETONATED;

            } else {

                Direction facing = world.getBlockState(pos).getValue(BlockStateProperties.HORIZONTAL_FACING);
                EntityFallingNuke bomb = new EntityFallingNuke(ModEntities.FALLING_NUKE.get(), world, v.tnt, v.nuke, v.hydro, v.amat, v.dirty, v.schrab, v.euph);
                bomb.setFacing(facing);
                bomb.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
                entity.clearSlots();
                world.removeBlock(pos, false);
                world.addFreshEntity(bomb);
                return BombReturnCode.TRIGGERED;
            }
        }

        return BombReturnCode.UNDEFINED;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeCustomBlock> CODEC = simpleCodec(NukeCustomBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
