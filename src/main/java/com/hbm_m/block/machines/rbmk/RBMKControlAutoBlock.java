package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlAutoBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RBMKControlAutoBlock extends RBMKColumnBlock {

    public final boolean moderated;
    /** See {@link RBMKControlManualBlock} - the original registers rbmk_control_auto and
     *  rbmk_control_reasim_auto as separate blocks with their own textures (ModBlocks.java:2106-2108). */
    private final String texturePrefix;

    public RBMKControlAutoBlock(boolean moderated, Properties props) {
        this(moderated, null, props);
    }

    public RBMKControlAutoBlock(boolean moderated, String texturePrefix, Properties props) {
        super(props);
        this.moderated = moderated;
        this.texturePrefix = texturePrefix;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        RBMKControlAutoBlockEntity be = new RBMKControlAutoBlockEntity(pos, state);
        be.moderated = moderated;
        be.texturePrefix = texturePrefix;
        // Only the two ReaSim variants need electricity to move; see RBMKControlBlockEntity.powered.
        be.powered = texturePrefix != null && texturePrefix.startsWith("rbmk_control_reasim");
        return be;
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_CONTROL_AUTO_BE.get(), RBMKControlAutoBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKControlAutoBlock> CODEC = simpleCodec(props -> new RBMKControlAutoBlock(false, props));

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
