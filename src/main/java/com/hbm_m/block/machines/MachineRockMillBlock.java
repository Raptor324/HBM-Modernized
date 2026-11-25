package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineRockMillBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineRockMill}: 5x5-Sockel {2,0,2,2,2,2} um den Kern, Offset 2, je zwei Anschluesse pro Seite.
 * Gezeichnet vom {@code RockMillRenderer}.
 */
public class MachineRockMillBlock extends DummyableMachineBlock {

    public MachineRockMillBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Frame is visible when blocks stand above the top ring. Lives in the BlockState so chunk meshing/culling see it. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty FRAME =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("frame");

    @Override
    protected BlockState withDefaults(BlockState state) {
        return state.setValue(FRAME, false);
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRAME);
    }

    @Override
    protected void onStructurePlaced(Level level, BlockPos core) {
        if (level.getBlockEntity(core) instanceof com.hbm_m.interfaces.IFrameSupportable be) {
            be.checkForFrame();
        }
    }

    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        return com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(2, 0, 2, 2, 2, 2)
                .extra(1, 0, 2)
                .extra(1, 0, -2)
                .extra(-1, 0, 2)
                .extra(-1, 0, -2)
                .extra(2, 0, 1)
                .extra(2, 0, -1)
                .extra(-2, 0, 1)
                .extra(-2, 0, -1)
                .placementOffset(2)
                .build(() -> com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineRockMillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ROCKMILL_BE.get(), MachineRockMillBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineRockMillBlockEntity computer) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, computer, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineRockMillBlock> CODEC = simpleCodec(MachineRockMillBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    // Verbatim port of the original MachineRockMill.getAllPorts.
    @Override
    public int[][] getAllPorts(Direction facing) {
        return rotatePorts(new int[][] {
            {2, 0, 1, Direction.EAST.get3DDataValue()},
            {-2, 0, 1, Direction.WEST.get3DDataValue()},
            {2, 0, -1, Direction.EAST.get3DDataValue()},
            {-2, 0, -1, Direction.WEST.get3DDataValue()},
            {1, 0, 2, Direction.SOUTH.get3DDataValue()},
            {1, 0, -2, Direction.NORTH.get3DDataValue()},
            {-1, 0, 2, Direction.SOUTH.get3DDataValue()},
            {-1, 0, -2, Direction.NORTH.get3DDataValue()}
        }, facing);
    }
}
