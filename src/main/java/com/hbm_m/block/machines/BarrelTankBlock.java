package com.hbm_m.block.machines;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

/**
 * Small single-block fluid barrel (no multiblock structure — same full-cube hitbox as the
 * decorative {@code CrtBlock} family it replaces for the functional iron/steel variants).
 * Fluid connects on all 6 sides with no restriction (the {@code MachineFluidTankBlockEntity}
 * logic it reuses only restricts sides when a multiblock explicitly sets them).
 * <p>
 * Parametrized by a BlockEntity factory + type supplier so each capacity variant (iron/steel)
 * can share this single Block class.
 */
public class BarrelTankBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1);

    private final BiFunction<BlockPos, BlockState, ? extends MachineFluidTankBlockEntity> beFactory;
    private final Supplier<BlockEntityType<? extends MachineFluidTankBlockEntity>> beTypeSupplier;
    @Nullable
    private final TooltipInfo tooltipInfo;

    /**
     * Static fluid-storage-capability summary shown as an item tooltip, matching the original
     * mod's per-material barrel info panel (capacity + hot/corrosive/highly-corrosive/antimatter
     * storage permissions). Declared statically here (rather than read off a live BlockEntity)
     * so it also shows up on the item in inventory/JEI, before the block is ever placed.
     */
    public record TooltipInfo(int capacityMb, boolean canStoreHot, boolean canStoreCorrosive,
                               boolean canStoreHighlyCorrosive, boolean canStoreAntimatter) {}

    public BarrelTankBlock(Properties properties,
                           BiFunction<BlockPos, BlockState, ? extends MachineFluidTankBlockEntity> beFactory,
                           Supplier<BlockEntityType<? extends MachineFluidTankBlockEntity>> beTypeSupplier) {
        this(properties, beFactory, beTypeSupplier, null);
    }

    public BarrelTankBlock(Properties properties,
                           BiFunction<BlockPos, BlockState, ? extends MachineFluidTankBlockEntity> beFactory,
                           Supplier<BlockEntityType<? extends MachineFluidTankBlockEntity>> beTypeSupplier,
                           @Nullable TooltipInfo tooltipInfo) {
        super(properties);
        this.beFactory = beFactory;
        this.beTypeSupplier = beTypeSupplier;
        this.tooltipInfo = tooltipInfo;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable BlockGetter level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (tooltipInfo == null) return;

        tooltip.add(Component.translatable("tooltip.hbm_m.barrel.capacity", tooltipInfo.capacityMb())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(storageLine(tooltipInfo.canStoreHot(), "tooltip.hbm_m.barrel.hot"));
        tooltip.add(storageLine(tooltipInfo.canStoreCorrosive(), "tooltip.hbm_m.barrel.corrosive"));
        tooltip.add(storageLine(tooltipInfo.canStoreHighlyCorrosive(), "tooltip.hbm_m.barrel.highly_corrosive"));
        tooltip.add(storageLine(tooltipInfo.canStoreAntimatter(), "tooltip.hbm_m.barrel.antimatter"));
    }

    private static Component storageLine(boolean can, String baseKey) {
        String key = can ? baseKey + ".yes" : baseKey + ".no";
        return Component.translatable(key).withStyle(can ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MachineFluidTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }

        MenuRegistry.openExtendedMenu((ServerPlayer) player, tank, buf -> buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return beFactory.apply(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, beTypeSupplier.get(), MachineFluidTankBlockEntity::tick);
    }
}
