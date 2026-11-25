package com.hbm_m.block.machines.rbmk;

import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.item.rbmk.RBMKToolItem;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RBMKCraneConsoleBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    public RBMKCraneConsoleBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.structureHelper = defineStructure();
    }

    private static MultiblockStructureHelper defineStructure() {
        // 1:1 with 1.7.10 RBMKCraneConsole.getDimensions() {1, 0, 0, 0, 1, 1} + fillSpace() {0, 0, 0, 1, 1, 1}:
        // 3 wide, 2 deep (front desk row + back panel row), 2 tall on the back panel row.
        String[] layer0 = {
                "OOO",
                "OCO"
        };
        String[] layer1 = {
                "   ",
                "UUU"
        };
        Map<Character, PartRole> roleMap = Map.of(
                'O', PartRole.DEFAULT,
                'U', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER
        );
        Map<Character, Supplier<BlockState>> symbolMap = Map.of();
        // Original RBMKCraneConsole: the UP dummy row is only 0.5 tall (1.5 blocks total).
        VoxelShape half = Block.box(0, 0, 0, 16, 8, 16);
        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { layer0, layer1 },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                Map.of('U', half),
                Map.of('U', half),
                null,
                null,
                null
        );
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper.resolvePartRole(localOffset, this);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
            if (level.getBlockEntity(pos) instanceof RBMKCraneConsoleBlockEntity crane) {
                crane.facing = state.getValue(FACING);
                crane.setChanged();
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            structureHelper.destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getControllerCellShape(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return structureHelper.getSpecificPartShape(structureHelper.getControllerOffset(), state.getValue(FACING));
    }

    // Crane console has no GUI (it's flown by keybind, not menu) - only the RBMK linking tool
    // and a screwdriver rotation cycle are handled here, bypassing RBMKColumnBlock's default
    // "open MenuProvider" behavior entirely.
    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RBMKCraneConsoleBlockEntity crane)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof RBMKToolItem) {
            RBMKToolItem.linkCrane(held, level, crane, player);
            return InteractionResult.SUCCESS;
        }

        // 1:1 with RBMKCraneConsole.onScrew(ToolType.SCREWDRIVER): a screwdriver steps the crane
        // rotation, sneak-clicking does nothing.
        if (held.getItem() instanceof com.hbm_m.item.tools_and_armor.ScrewdriverItem) {
            crane.cycleCraneRotation();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RBMKCraneConsoleBlockEntity crane)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() instanceof RBMKToolItem) {
            RBMKToolItem.linkCrane(held, level, crane, player);
            return InteractionResult.SUCCESS;
        }

        // 1:1 with RBMKCraneConsole.onScrew(ToolType.SCREWDRIVER): a screwdriver steps the crane
        // rotation, sneak-clicking does nothing.
        if (held.getItem() instanceof com.hbm_m.item.tools_and_armor.ScrewdriverItem) {
            crane.cycleCraneRotation();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
        }
    *///?}


    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKCraneConsoleBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RBMK_CRANE_CONSOLE_BE.get(), RBMKCraneConsoleBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RBMKCraneConsoleBlock> CODEC = simpleCodec(RBMKCraneConsoleBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
