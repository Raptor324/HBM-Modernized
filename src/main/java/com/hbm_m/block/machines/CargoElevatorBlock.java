package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CargoElevatorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Self-stacking 3x3-footprint elevator shaft. Right-clicking with another elevator block in hand
 * adds a floor (a new 3x3 layer above the current top); right-clicking otherwise toggles the
 * platform between fully extended and fully retracted. Only the bottom-center block of the shaft
 * is the {@link CargoElevatorBlockEntity} core — every other cell is a dummy part pointing back at
 * it (see {@link com.hbm_m.multiblock.DummyCoreBlockEntity}). Visuals are drawn entirely by
 * {@code CargoElevatorRenderer} from the core; collision is computed per-cell in {@link #getShape}.
 */
public class CargoElevatorBlock extends BaseEntityBlock {

    private static final double POST = 0.25;
    private static final double PLATFORM_THICKNESS = 0.125;

    public CargoElevatorBlock(Properties properties) {
        super(properties);
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

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return computeShape(level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return computeShape(level, pos);
    }

    private VoxelShape computeShape(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator)) {
            return Shapes.block();
        }
        BlockPos corePos = elevator.getCorePos();
        int dx = pos.getX() - corePos.getX();
        int dz = pos.getZ() - corePos.getZ();
        if (dx < -1 || dx > 1 || dz < -1 || dz > 1) {
            return Shapes.block();
        }

        VoxelShape shape = Shapes.empty();

        // Corner guide posts: always present, one per floor, thin verticals at the 4 outer corners.
        if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
            double x0 = dx < 0 ? 0.0 : 1.0 - POST;
            double x1 = dx < 0 ? POST : 1.0;
            double z0 = dz < 0 ? 0.0 : 1.0 - POST;
            double z1 = dz < 0 ? POST : 1.0;
            shape = Shapes.join(shape, Shapes.box(x0, 0, z0, x1, 1, z1), BooleanOp.OR);
        }

        // Moving platform: a thin slab that lives in whichever floor currently contains it.
        CargoElevatorBlockEntity core = elevator.resolveCore(CargoElevatorBlockEntity.class);
        if (core != null) {
            double platformWorldY = corePos.getY() + 1 + core.extension;
            double localY = platformWorldY - pos.getY();
            if (localY > -PLATFORM_THICKNESS && localY < 1.0) {
                double y0 = Mth.clamp(localY, 0.0, 1.0 - PLATFORM_THICKNESS);
                double y1 = Mth.clamp(localY + PLATFORM_THICKNESS, PLATFORM_THICKNESS, 1.0);
                shape = Shapes.join(shape, Shapes.box(0, y0, 0, 1, y1, 1), BooleanOp.OR);
            }
        }

        return shape;
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
        CargoElevatorBlockEntity core = elevator.resolveCore(CargoElevatorBlockEntity.class);
        if (core == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() == this.asItem()) {
            if (tryAddFloor(level, core)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, core.getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        core.toggleElevator();
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
        CargoElevatorBlockEntity core = elevator.resolveCore(CargoElevatorBlockEntity.class);
        if (core == null) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEmpty() && held.getItem() == this.asItem()) {
            if (tryAddFloor(level, core)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, core.getBlockPos(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.8F);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        core.toggleElevator();
        return InteractionResult.SUCCESS;
        }
    *///?}


    // ─── audit10: 1:1 BlockDummyable-Verhalten (Dimensionen {0,0,1,1,1,1}, Offset 1) ───

    /** Original {@code onBlockPlacedBy}: der Kern liegt einen Block in Blickrichtung, das 3x3 darum muss frei sein. */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        BlockPos core = ctx.getClickedPos().relative(ctx.getHorizontalDirection());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = core.offset(dx, 0, dz);
                if (!p.equals(ctx.getClickedPos()) && !ctx.getLevel().getBlockState(p).canBeReplaced(ctx)) return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || placer == null) return;
        BlockPos core = pos.relative(placer.getDirection());

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = core.offset(dx, 0, dz);
                if (!p.equals(pos)) level.setBlock(p, defaultBlockState(), 3);
                if (level.getBlockEntity(p) instanceof CargoElevatorBlockEntity part) part.setCorePos(core);
            }
        }
    }

    /** Laeuft gerade der Abriss des ganzen Schachts (Original: Waisen-Dummies verschwinden ohne Drop). */
    private static final ThreadLocal<Boolean> CASCADE = ThreadLocal.withInitial(() -> false);

    /** Original {@code getDrops}: der Spieler bekommt height + 1 Aufzugsbloecke (64er-Stapel). */
    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropShaft(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropShaft(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void dropShaft(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || player.getAbilities().instabuild) return;
        if (!(level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator)) return;
        CargoElevatorBlockEntity core = elevator.resolveCore(CargoElevatorBlockEntity.class);
        if (core == null) return;
        int toDrop = core.height + 1;
        while (toDrop > 0) {
            int perStack = Math.min(toDrop, 64);
            toDrop -= perStack;
            popResource(level, pos, new ItemStack(this, perStack));
        }
    }

    /** Original {@code BlockDummyable.breakBlock}/Waisenpruefung: faellt eine Zelle weg, verschwindet der ganze Schacht. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && !CASCADE.get()
                && level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
            BlockPos core = elevator.getCorePos();
            CargoElevatorBlockEntity coreBe = elevator.resolveCore(CargoElevatorBlockEntity.class);
            int height = coreBe != null ? coreBe.height : 0;
            CASCADE.set(true);
            try {
                for (int dy = 0; dy <= height; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            BlockPos p = core.offset(dx, dy, dz);
                            if (!p.equals(pos) && level.getBlockState(p).is(this)) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            } finally {
                CASCADE.set(false);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private boolean tryAddFloor(Level level, CargoElevatorBlockEntity core) {
        BlockPos corePos = core.getBlockPos();
        int newY = corePos.getY() + core.height + 1;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(corePos.getX() + dx, newY, corePos.getZ() + dz);
                if (!level.getBlockState(p).canBeReplaced()) {
                    return false;
                }
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(corePos.getX() + dx, newY, corePos.getZ() + dz);
                level.setBlock(p, this.defaultBlockState(), 3);
                if (level.getBlockEntity(p) instanceof CargoElevatorBlockEntity part && !p.equals(corePos)) {
                    part.setCorePos(corePos);
                }
            }
        }

        core.addFloor();
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
