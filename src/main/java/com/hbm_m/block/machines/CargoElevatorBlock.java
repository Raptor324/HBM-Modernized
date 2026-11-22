package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;
import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Self-stacking 3x3-footprint elevator shaft.
 * Right-clicking with another elevator block adds a floor;
 * right-clicking otherwise toggles the platform between fully extended and fully retracted.
 * Controller is in the bottom center; all 8 surrounding cells and all extra floors are
 * {@link com.hbm_m.block.UniversalMachinePartBlock}.
 */
public class CargoElevatorBlock extends DummyableMachineBlock {

    public static final double PLATFORM_THICKNESS = 0.25;

    public CargoElevatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        String[][] layers = {
            {
                "AAA",
                "ACA",
                "AAA"
            }
        };

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'A', PartRole.DEFAULT
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
            'A', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            layers,
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap
        );
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoElevatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CARGO_ELEVATOR_BE.get(), CargoElevatorBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public static VoxelShape buildMasterShape(@Nullable CargoElevatorBlockEntity elevator) {
        double height = (elevator != null) ? (elevator.height + 1.0) : 1.0;
        double ext = (elevator != null) ? elevator.extension : 0.0;

        VoxelShape posts = Shapes.or(
            Shapes.box(-1.0, 0.0, -1.0, -0.75, height, -0.75),
            Shapes.box(-1.0, 0.0, 1.75, -0.75, height, 2.0),
            Shapes.box(1.75, 0.0, -1.0, 2.0, height, -0.75),
            Shapes.box(1.75, 0.0, 1.75, 2.0, height, 2.0)
        );

        double slabBottom = 1.0 - PLATFORM_THICKNESS + ext;
        double slabTop = 1.0 + ext;
        VoxelShape platform = Shapes.box(-1.0, slabBottom, -1.0, 2.0, slabTop, 2.0);

        return Shapes.or(posts, platform);
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (level != null && pos != null && level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
            return buildMasterShape(elevator);
        }
        return buildMasterShape(null);
    }

    @Override
    public VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return buildMasterShape(null);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape master = getCustomMasterVoxelShape(state, level, pos);
        return MultiblockStructureHelper.cellShape(master, BlockPos.ZERO);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape master = getCustomMasterVoxelShape(state, level, pos);
        return MultiblockStructureHelper.cellShape(master, BlockPos.ZERO);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() == this.asItem()) {
            if (tryAddFloor(level, pos, elevator)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        elevator.toggleElevator();
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEmpty() && held.getItem() == this.asItem()) {
            if (tryAddFloor(level, pos, elevator)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        elevator.toggleElevator();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!held.isEmpty() && held.getItem() == this.asItem()) {
            if (level.isClientSide) {
                return net.minecraft.world.ItemInteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
                if (tryAddFloor(level, pos, elevator)) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
                    return net.minecraft.world.ItemInteractionResult.CONSUME;
                }
                return net.minecraft.world.ItemInteractionResult.FAIL;
            }
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    public boolean tryAddFloor(Level level, BlockPos corePos, CargoElevatorBlockEntity core) {
        int newY = corePos.getY() + core.height + 1;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(corePos.getX() + dx, newY, corePos.getZ() + dz);
                if (!level.getBlockState(p).canBeReplaced()) {
                    return false;
                }
            }
        }

        BlockState partState = ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(corePos.getX() + dx, newY, corePos.getZ() + dz);
                level.setBlock(p, partState, 3);
                if (level.getBlockEntity(p) instanceof IMultiblockPart part) {
                    part.setControllerPos(corePos);
                    part.setLocalOffsetFromController(new BlockPos(dx, core.height + 1, dz));
                }
            }
        }

        core.addFloor();
        return true;
    }

    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
                elevator.dropped = true;
                if (!player.getAbilities().instabuild) {
                    dropShaft(level, pos, elevator.height + 1);
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
                elevator.dropped = true;
                if (!player.getAbilities().instabuild) {
                    dropShaft(level, pos, elevator.height + 1);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void dropShaft(Level level, BlockPos pos, int count) {
        int toDrop = count;
        while (toDrop > 0) {
            int perStack = Math.min(toDrop, 64);
            toDrop -= perStack;
            popResource(level, pos, new ItemStack(this, perStack));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            MultiblockStructureHelper helper = getStructureHelper();
            Direction facing = state.getValue(FACING);
            int height = 0;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CargoElevatorBlockEntity elevator) {
                height = elevator.height;
                if (!elevator.dropped) {
                    elevator.dropped = true;
                    dropShaft(level, pos, height + 1);
                }
            }
            if (helper != null) {
                helper.destroyStructure(level, pos, facing);
            }
            // Clear extra floors
            for (int dy = 1; dy <= height; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos p = pos.offset(dx, dy, dz);
                        if (level.getBlockState(p).is(ModBlocks.UNIVERSAL_MACHINE_PART.get())) {
                            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static final int MAX_SHAFT_SCAN = 384;

    /**
     * Re-derives the core from the shaft's own geometry: walk down the contiguous column of
     * elevator blocks, then look for the core in the 3x3 around the bottom of it.
     */
    @Nullable
    public static BlockPos findCoreByGeometry(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < MAX_SHAFT_SCAN; i++) {
            BlockPos below = cursor.below();
            BlockState stateBelow = level.getBlockState(below);
            if (!(stateBelow.getBlock() instanceof CargoElevatorBlock || stateBelow.getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock)) break;
            cursor.move(Direction.DOWN);
        }
        int bottomY = cursor.getY();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(pos.getX() + dx, bottomY, pos.getZ() + dz);
                if (level.getBlockEntity(p) instanceof CargoElevatorBlockEntity be && be.isCore()) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Screwdriver shift-click dismantle: removes the top 3x3 floor of the shaft and returns one
     * elevator block (the same amount {@link #tryAddFloor} consumed) in survival. Clicking any
     * part of the shaft - core or cell - shortens it by one floor, so the structure never has
     * to be rebuilt.
     */
    public static boolean dismantleTopFloor(Level level, BlockPos corePos, CargoElevatorBlockEntity core, Player player) {
        if (core.height <= 0 || !core.isCore()) {
            return false;
        }
        int layerY = corePos.getY() + core.height;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(corePos.getX() + dx, layerY, corePos.getZ() + dz);
                if (!(level.getBlockState(p).getBlock() instanceof com.hbm_m.block.UniversalMachinePartBlock)) {
                    return false;
                }
            }
        }
        // Removing a part normally cascades into destroying the whole structure
        // (shouldDestroyOnPartRemoved). runSafeRemove (1:1 BlockDummyable.safeRem) suppresses
        // that cascade so only this floor is dismantled.
        com.hbm_m.multiblock.MultiblockStructureHelper.runSafeRemove(() -> {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = new BlockPos(corePos.getX() + dx, layerY, corePos.getZ() + dz);
                    level.removeBlock(p, false);
                }
            }
        });
        core.removeFloor();
        if (!level.isClientSide) {
            BlockPos layerCenter = new BlockPos(corePos.getX(), layerY, corePos.getZ());
            if (!player.getAbilities().instabuild) {
                popResource(level, layerCenter, new ItemStack(level.getBlockState(corePos).getBlock()));
            }
            level.playSound(null, layerCenter, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return true;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<CargoElevatorBlock> CODEC = simpleCodec(CargoElevatorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
