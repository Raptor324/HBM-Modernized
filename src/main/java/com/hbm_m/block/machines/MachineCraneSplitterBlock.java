package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.blockentity.network.MachineCraneSplitterBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code CraneSplitter} ({@code BlockDummyable}, Dimensionen {0,0,0,0,0,1}): zwei Bloecke breites Band. Der Kern
 * ist immer die linke Haelfte (in Laufrichtung), die rechte liegt {@code dir.getRotation(DOWN)} daneben. Teile laufen nur
 * aus Laufrichtung hinein und werden im Verhaeltnis links:rechts auf beide Haelften verteilt; der Schraubendreher stellt
 * auf der jeweiligen Haelfte deren Anteil (1-16, schleichend -1) ein.
 */
public class MachineCraneSplitterBlock extends BaseEntityBlock implements IConveyorBelt, IEnterableBlock, IToolable,
        com.hbm_m.interfaces.ILookOverlay {

    public enum Half implements StringRepresentable {
        LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    /** Original: {@code dir} des Kerns - die Teile laufen entgegen dieser Richtung. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = EnumProperty.create("half", Half.class);
    /** Original: {@code setBlockBounds(0, 0, 0, 1, 0.999, 1)}. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15.984, 16);

    public MachineCraneSplitterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, Half.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    /** Rechte Haelfte relativ zum Kern: {@code dir.getRotation(ForgeDirection.DOWN)}. */
    public static Direction rightSide(Direction dir) {
        return dir.getCounterClockWise();
    }

    @Nullable
    public static BlockPos findCore(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof MachineCraneSplitterBlock)) return null;
        if (s.getValue(HALF) == Half.LEFT) return pos;
        return pos.relative(rightSide(s.getValue(FACING)).getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection().getOpposite();
        BlockPos right = ctx.getClickedPos().relative(rightSide(dir));
        if (!ctx.getLevel().getBlockState(right).canBeReplaced(ctx)) return null;
        return defaultBlockState().setValue(FACING, dir).setValue(HALF, Half.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) return;
        level.setBlock(pos.relative(rightSide(state.getValue(FACING))), state.setValue(HALF, Half.RIGHT), 3);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            Direction dir = state.getValue(FACING);
            BlockPos other = state.getValue(HALF) == Half.LEFT ? pos.relative(rightSide(dir)) : pos.relative(rightSide(dir).getOpposite());
            BlockState os = level.getBlockState(other);
            if (os.getBlock() == this && os.getValue(FACING) == dir && os.getValue(HALF) != state.getValue(HALF)) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == Half.LEFT ? new MachineCraneSplitterBlockEntity(pos, state) : null;
    }

    // ==================== IConveyorBelt / IEnterableBlock ====================

    public Direction getTravelDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorItemEntity item) {
        return getTravelDirection(level, pos) == dir;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity entity) {
        BlockPos core = findCore(level, pos);
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof MachineCraneSplitterBlockEntity splitter)) return;
        Direction rot = rightSide(level.getBlockState(core).getValue(FACING));

        ItemStack[] splits = splitter.splitStack(entity.getItem());
        entity.discard();

        spawnMovingItem(level, core, splits[0]);
        spawnMovingItem(level, core.relative(rot), splits[1]);
    }

    private void spawnMovingItem(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0) return;
        Vec3 snap = getClosestSnappingPosition(level, pos, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        level.addFreshEntity(MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, stack));
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos);
        Vec3 snap = getClosestSnappingPosition(level, pos, itemPos);
        Vec3 dest = new Vec3(snap.x - dir.getStepX() * speed, snap.y - dir.getStepY() * speed, snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        return new Vec3(itemPos.x + motion.x / len * speed, itemPos.y + motion.y / len * speed, itemPos.z + motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos);
        double ix = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1);
        double iz = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1);
        double posX = pos.getX() + 0.5, posZ = pos.getZ() + 0.5;
        if (dir.getStepX() != 0) posX = ix;
        if (dir.getStepZ() != 0) posZ = iz;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    // ==================== Schraubendreher / Overlay / Info ====================

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (world.isClientSide) return true;
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockPos core = findCore(world, pos);
        if (core == null) return false;
        if (!(world.getBlockEntity(core) instanceof MachineCraneSplitterBlockEntity crane)) return false;

        // Der Kern ist immer die linke Haelfte
        boolean isLeft = pos.equals(core);
        int adjust = player.isShiftKeyDown() ? -1 : 1;

        if (isLeft) {
            crane.leftRatio = (byte) Mth.clamp(crane.leftRatio + adjust, 1, 16);
        } else {
            crane.rightRatio = (byte) Mth.clamp(crane.rightRatio + adjust, 1, 16);
        }
        crane.sync();
        return true;
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachineCraneSplitterBlockEntity crane)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Splitter ratio: " + crane.leftRatio + ":" + crane.rightRatio));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneSplitterBlock> CODEC = simpleCodec(MachineCraneSplitterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
