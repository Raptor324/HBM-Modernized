package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCyclotronBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MachineCyclotronBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public MachineCyclotronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructureNew();
    }

    private static MultiblockStructureHelper defineStructureNew() {
        // GIT MachineCyclotron: 5×5×5 solid core + outer ring connectors at y=0
        String[] layer0 = {
            "OBBBO",
            "BOOOB",
            "BOCOB",
            "BOOOB",
            "OBBBO"
        };
        String[] layerSolid = {
            "OOOOO",
            "OOOOO",
            "OOOOO",
            "OOOOO",
            "OOOOO"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'O', PartRole.DEFAULT,
            'B', PartRole.UNIVERSAL_CONNECTOR,
            'C', PartRole.CONTROLLER
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        Map<Character, boolean[]> energySideMap = Map.of(
            'B', MultiblockSideTuples.energy(true, true, true, true, true, false),
            'C', MultiblockSideTuples.energy(true, true, true, true, true, false)
        );
        Map<Character, boolean[]> fluidSideMap = Map.of(
            'B', MultiblockSideTuples.fluid(true, true, true, true, true, false),
            'C', MultiblockSideTuples.fluid(true, true, true, true, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRolesAndSides(
            new String[][]{layer0, layerSolid, layerSolid, layerSolid, layerSolid},
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            null,
            energySideMap,
            fluidSideMap
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        if (structureHelper != null) {
            return structureHelper.resolvePartRole(localOffset, this);
        }
        return PartRole.DEFAULT;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null && !helper.isFullBlock(helper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCyclotronBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CYCLOTRON_BE.get(), MachineCyclotronBlockEntity::tick);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            BlockPos core = placeMultiblockStructure(level, pos, state);
            if (core == null) {
                return;
            }
        }
    }


    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            getStructureHelper().destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
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
        if (!level.isClientSide()) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof MenuProvider menuProvider) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            return helper.generateShapeFromParts(state.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null) {
            return helper.getSpecificPartShape(helper.getControllerOffset(), state.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCyclotronBlock> CODEC = simpleCodec(MachineCyclotronBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
