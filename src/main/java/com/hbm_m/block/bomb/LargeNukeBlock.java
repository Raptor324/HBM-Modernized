package com.hbm_m.block.bomb;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.LargeNukeBlockEntity;
import com.hbm_m.explosion.NuclearExplosionAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Большая ядерная бомба (Gadget / Little Boy / Ivy Mike / Tsar Bomba).
 * Один класс блока на все типы, вариант определяется полем type.
 */
public class LargeNukeBlock extends NukeBaseBlock implements IBomb {

    public final LargeNukeType type;

    public LargeNukeBlock(LargeNukeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeNukeBlockEntity(pos, state, type);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    /** Резервный путь (радиус по типу), основной — через переопределённый explode(BlockPos). */
    @Override
    protected void explode(Level level, double x, double y, double z) {
        NuclearExplosionAPI.startLargeNuke(level, x, y, z,
                type.detonationRadius(new net.minecraft.world.item.ItemStack[0]));
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof LargeNukeBlockEntity nuke) {
            // Радиус считаем ДО очистки контейнера (Tsar без tsar_core бьёт по радиусу Fat Man).
            net.minecraft.world.item.ItemStack[] items = nuke.slots.toArray(new net.minecraft.world.item.ItemStack[0]);
            int radius = type.detonationRadius(items);
            if (nuke.isReady()) {
                Containers.dropContents(level, pos, nuke);
                nuke.clearContent();
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                NuclearExplosionAPI.startLargeNuke(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        return BombReturnCode.UNDEFINED;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LargeNukeBlock> CODEC =
            simpleCodec(props -> new LargeNukeBlock(LargeNukeType.GADGET, props));
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
