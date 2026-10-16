package com.hbm_m.block.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKControlManualBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RBMKControlManualBlock extends RBMKColumnBlock {

    public final boolean moderated;
    /** Texture set for this variant. The original registers rbmk_control, rbmk_control_mod and
     *  rbmk_control_reasim as separate blocks, each with its own texture (ModBlocks.java:2104-2107);
     *  null means "derive from the moderated flag" for the two non-reasim variants. */
    private final String texturePrefix;

    public RBMKControlManualBlock(boolean moderated, Properties props) {
        this(moderated, null, props);
    }

    public RBMKControlManualBlock(boolean moderated, String texturePrefix, Properties props) {
        super(props);
        this.moderated = moderated;
        this.texturePrefix = texturePrefix;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        RBMKControlManualBlockEntity be = new RBMKControlManualBlockEntity(pos, state);
        be.moderated = moderated;
        be.texturePrefix = texturePrefix;
        // Only the two ReaSim variants need electricity to move; see RBMKControlBlockEntity.powered.
        be.powered = texturePrefix != null && texturePrefix.startsWith("rbmk_control_reasim");
        return be;
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_CONTROL_BE.get(), RBMKControlManualBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKControlManualBlock> CODEC = simpleCodec(props -> new RBMKControlManualBlock(false, props));

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
