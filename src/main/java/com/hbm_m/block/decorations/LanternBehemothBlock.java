package com.hbm_m.block.decorations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.api.tile.IRepairable;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.LanternBehemothBlockEntity;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockLanternBehemoth}: Dummyable {@code {4,0,0,0,0,0}}, Offset 0, laesst nichts fallen. Reparatur mit
 * dem Schweissbrenner hebt den Ruf des Spielers (bis 25); die Blickhilfe zeigt die Reparaturmaterialien.
 */
public class LanternBehemothBlock extends DummyableMachineBlock implements IToolable, com.hbm_m.interfaces.ILookOverlay {

    public LanternBehemothBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(4, 0, 0, 0, 0, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LanternBehemothBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LANTERN_BEHEMOTH_BE.get(), LanternBehemothBlockEntity::tick);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {

        if (tool != ToolType.TORCH) return false;
        boolean didRepair = IRepairable.tryRepairMultiblock(world, pos, player);

        if (didRepair && !world.isClientSide) {
            HbmPlayerProps data = HbmPlayerProps.get(player);
            if (data.reputation < 25) data.reputation++;
        }

        return didRepair;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        IRepairable.addGenericOverlay(g, world, pos, Component.translatable(getDescriptionId()));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LanternBehemothBlock> CODEC = simpleCodec(LanternBehemothBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
