package com.hbm_m.block.machines.albion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.albion.PABeamlineBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code BlockPABeamline} (1.7.10): das blosse Strahlrohr.
 *
 * <p>Drei Felder lang entlang der Strahlachse - je eine Dummyzelle vor und hinter dem Kern. Das
 * Teilchen tritt an der vorderen ein und verlaesst das Rohr zwei Felder hinter dem Kern, also
 * genau dort, wo das naechste Bauteil seine Eingangszelle hat.</p>
 *
 * <p>Der Schraubendreher schaltet das Sichtfenster um.</p>
 */
public class PABeamlineBlock extends PAMultiblockBlock {

    public PABeamlineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: BlockPABeamline - getDimensions {0,0,0,0,1,1}, getOffset 0, keine Zusatzzellen.
        return DummyableStructureBuilder.create()
                .box(0, 0, 0, 0, 1, 1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** 1:1-Port von {@code onScrew}: der Schraubendreher schaltet das Sichtfenster um. */
    @Override
    protected net.minecraft.world.InteractionResult interact(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {

        if (!player.getItemInHand(hand).is(com.hbm_m.item.ModItems.SCREWDRIVER.get())) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof PABeamlineBlockEntity beamline) {
            beamline.setWindow(!beamline.hasWindow());
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PABeamlineBlockEntity(pos, state);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PABeamlineBlock> CODEC = simpleCodec(PABeamlineBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
