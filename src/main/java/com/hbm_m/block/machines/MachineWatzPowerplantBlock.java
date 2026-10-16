package com.hbm_m.block.machines;

import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineWatzPowerplantBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
//?}
import dev.architectury.registry.menu.MenuRegistry;

/**
 * Watz Powerplant - true multiblock port of the original 1.7.10 {@code Watz}/{@code TileEntityWatzStruct}
 * (a {@code BlockDummyable}-based cross/octagon dummy-block tower built from {@code watz_element}/
 * {@code watz_cooler}/{@code watz_end} rings), built instead on this repo's own
 * {@link IMultiblockController}/{@link MultiblockStructureHelper} framework (the same pattern used by
 * {@code MachineZirnoxBlock}/{@code MachineArcFurnaceBlock}), NOT the obsolete dummy-block system.
 * <p>
 * SCOPE: the original stacked multiple 3-tall segments into a tower with pellets falling between
 * segments (see class doc on {@link MachineWatzPowerplantBlockEntity}). This port implements a single
 * 5x5x3 segment (25 cells per layer x 3 layers = 75 parts incl. controller), matching the original's
 * per-segment footprint scale without the multi-segment stacking. The fluid-connector ring cells
 * (perimeter mid-edge cells on every layer) mirror the original's {@code subscribeToTop}/
 * {@code sendOutBottom} behaviour: any adjacent pipe can both push cold coolant in and pull hot
 * coolant/waste out (direction is resolved by fluid type in the block entity, not by cell position -
 * simpler than the original's separate top-only/bottom-only positions, but functionally equivalent).
 */
public class MachineWatzPowerplantBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineWatzPowerplantBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        String[] layerRing = {
            "OOOOO",
            "OOOOO",
            "FOOOF",
            "OOOOO",
            "OOOOO"
        };
        String[] layerController = {
            "OOOOO",
            "OOOOO",
            "FOCOF",
            "OOOOO",
            "OOOOO"
        };

        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'F', PartRole.FLUID_CONNECTOR,
                'C', PartRole.CONTROLLER
        );

        Map<Character, boolean[]> fluidSideMap = Map.of(
                'C', MultiblockSideTuples.fluid(true, true, true, true, true, false),
                'F', MultiblockSideTuples.fluid(true, true, true, true, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
                new String[][] { layerRing, layerController, layerRing },
                null,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null,
                fluidSideMap
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificPartShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (!structureHelper.isFullBlock(structureHelper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            structureHelper.placeStructure(level, pos, state.getValue(FACING), this);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) return;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MachineWatzPowerplantBlockEntity watz)) return;

        boolean powered = false;
        for (int dx = -2; dx <= 2 && !powered; dx++) {
            for (int dy = 0; dy <= 2 && !powered; dy++) {
                for (int dz = -2; dz <= 2 && !powered; dz++) {
                    boolean isSurface = dx == -2 || dx == 2 || dy == 0 || dy == 2 || dz == -2 || dz == 2;
                    if (!isSurface) continue;

                    BlockPos scanPos = pos.offset(dx, dy, dz);
                    if (level.hasNeighborSignal(scanPos) || level.getBestNeighborSignal(scanPos) > 0) {
                        powered = true;
                        break;
                    }
                }
            }
        }

        watz.setRedstonePowered(powered);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            //? if forge {
            if (be != null) be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
                for (int i = 0; i < h.getSlots(); i++)
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), h.getStackInSlot(i));
            });
            //?} elif neoforge {
            /*var h = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, state, be, null);
            if (h != null) {
                for (int i = 0; i < h.getSlots(); i++)
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), h.getStackInSlot(i));
            }
            *///?}
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineWatzPowerplantBlockEntity(pos, state);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }
    *///?}

    private InteractionResult openMenu(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider p)
            MenuRegistry.openExtendedMenu((ServerPlayer) player, p, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.WATZ_POWERPLANT_BE.get(), MachineWatzPowerplantBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineWatzPowerplantBlock> CODEC = simpleCodec(MachineWatzPowerplantBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
