package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineConveyorPressBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Conveyor Press - Einzelblock-Port (siehe Klassenkommentar in
 * {@link MachineConveyorPressBlockEntity} fuer die Scope-Entscheidungen). Kein GUI: Rechtsklick mit
 * Stempel installiert ihn, Rechtsklick mit Schraubenzieher entfernt ihn wieder (1:1 aus dem
 * Original {@code onBlockActivated}/{@code onScrew}).
 */
public class MachineConveyorPressBlock extends com.hbm_m.block.machines.DummyableMachineBlock
        implements com.hbm_m.block.network.IConveyorBelt {

    /**
     * 1:1-Port von {@code MachineConveyorPress} (1.7.10): drei Felder hoch
     * ({@code getDimensions {2,0,0,0,0,0}}), und die <b>Oberseite ist selbst ein Foerderband</b>.
     *
     * <p>Das ist der Sinn der Maschine: Gegenstaende laufen oben durch und werden im Vorbeigehen
     * gepresst - man haengt sie mitten in eine Bandstrecke, statt sie zu befuellen.</p>
     */
    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        return com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(2, 0, 0, 0, 0, 0)
                .placementOffset(0)
                .build(() -> com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    // ── Foerderband auf der Oberseite ──

    /** 1:1: ein Gegenstand bleibt nur oben auf der Presse liegen, nicht daneben. */
    @Override
    public boolean canItemStay(Level level, BlockPos pos, net.minecraft.world.phys.Vec3 itemPos) {
        return level.getBlockState(pos.below()).getBlock() == this
                || level.getBlockEntity(pos.below()) instanceof MachineConveyorPressBlockEntity;
    }

    @Override
    public net.minecraft.world.phys.Vec3 getTravelLocation(Level level, BlockPos pos,
            net.minecraft.world.phys.Vec3 itemPos, double speed) {

        Direction dir = travelDirection(level, pos);
        net.minecraft.world.phys.Vec3 snap = getClosestSnappingPosition(level, pos, itemPos);
        net.minecraft.world.phys.Vec3 dest = new net.minecraft.world.phys.Vec3(
                snap.x - dir.getStepX() * speed, snap.y - dir.getStepY() * speed, snap.z - dir.getStepZ() * speed);

        net.minecraft.world.phys.Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        if (len < 1.0E-6D) return itemPos;

        return itemPos.add(motion.scale(speed / len));
    }

    @Override
    public net.minecraft.world.phys.Vec3 getClosestSnappingPosition(Level level, BlockPos pos,
            net.minecraft.world.phys.Vec3 itemPos) {

        Direction dir = travelDirection(level, pos);

        double clampedX = Math.max(pos.getX(), Math.min(pos.getX() + 1, itemPos.x));
        double clampedZ = Math.max(pos.getZ(), Math.min(pos.getZ() + 1, itemPos.z));

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) posX = clampedX;
        if (dir.getStepZ() != 0) posZ = clampedZ;

        return new net.minecraft.world.phys.Vec3(posX, pos.getY() + 0.25D, posZ);
    }

    /** Die Laufrichtung steht am Kern - die Dummyzellen daruber tragen keine eigene. */
    private Direction travelDirection(Level level, BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState own = level.getBlockState(pos);
        if (own.hasProperty(FACING)) return own.getValue(FACING);

        for (int down = 1; down <= 2; down++) {
            net.minecraft.world.level.block.state.BlockState below = level.getBlockState(pos.below(down));
            if (below.getBlock() == this && below.hasProperty(FACING)) return below.getValue(FACING);
        }
        return Direction.NORTH;
    }
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public MachineConveyorPressBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineConveyorPressBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineConveyorPressBlockEntity press) {
                press.dropInventoryContents();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MachineConveyorPressBlockEntity press)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            ItemStack removed = press.removeStamp();
            if (!removed.isEmpty() && !player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
            return InteractionResult.CONSUME;
        }

        if (press.tryInsertStamp(player, held)) {
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MachineConveyorPressBlockEntity press)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            ItemStack removed = press.removeStamp();
            if (!removed.isEmpty() && !player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
            return InteractionResult.CONSUME;
        }

        if (press.tryInsertStamp(player, held)) {
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
        }
    *///?}


    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CONVEYOR_PRESS_BE.get(), MachineConveyorPressBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineConveyorPressBlock> CODEC = simpleCodec(MachineConveyorPressBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
