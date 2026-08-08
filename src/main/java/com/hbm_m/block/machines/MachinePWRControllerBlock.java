package com.hbm_m.block.machines;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PWRControllerBlockEntity;
import com.hbm_m.blockentity.machines.PWRPartBlockEntity;
import com.hbm_m.blockentity.machines.PWRPartBlockEntity.Kind;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraftforge.network.NetworkHooks;

/**
 * PWR reactor controller. 1:1 port of {@code com.hbm.blocks.machine.MachinePWRController}
 * (1.7.10): right-clicking an unassembled controller flood-fills the structure in front of it
 * (in its facing direction) exactly like the original's {@code assemble}/{@code floodFill}, and
 * right-clicking an assembled one opens the GUI (unless holding the PWR Printer, which takes over
 * the click instead - see {@code PWRFuelPrinterItem}).
 * <p>
 * Unlike the original (which rewrites every matched part into a generic {@code pwr_block} carrier
 * remembering its original type), assembly here just points each part's
 * {@link PWRPartBlockEntity} at this controller via {@code setCorePos} - see that class's doc.
 * <p>
 * Error feedback uses a chat message instead of the original's in-world particle/label marker
 * packet (a minor UX-only substitution, not a mechanic).
 */
public class MachinePWRControllerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final int MAX_ASSEMBLY_SIZE = 4096;

    public MachinePWRControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller) {
            return controller.getComparatorPower();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PWRControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PWR_CONTROLLER_BE.get(), PWRControllerBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }

        if (!controller.assembled) {
            assemble(level, pos, state.getValue(FACING), player);
            return InteractionResult.SUCCESS;
        }

        if (player.getItemInHand(hand).getItem() == ModItems.PWR_PRINTER.get()) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            NetworkHooks.openScreen((ServerPlayer) player, menuProvider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // ── Assembly (1:1 port of MachinePWRController.assemble/floodFill) ────────

    public void assemble(Level level, BlockPos controllerPos, Direction facing, @Nullable Player player) {
        Map<BlockPos, Kind> assembly = new HashMap<>();
        Set<BlockPos> fuelRods = new HashSet<>();
        Set<BlockPos> sources = new HashSet<>();
        boolean[] errored = { false };

        Direction dir = facing.getOpposite();
        floodFill(level, controllerPos.relative(dir), assembly, fuelRods, sources, errored, player);

        if (fuelRods.isEmpty()) {
            sendError(level, controllerPos, "Fuel rods required", player);
            errored[0] = true;
        }
        if (sources.isEmpty()) {
            sendError(level, controllerPos, "Neutron sources required", player);
            errored[0] = true;
        }

        if (!(level.getBlockEntity(controllerPos) instanceof PWRControllerBlockEntity controller)) {
            return;
        }

        if (!errored[0]) {
            for (Map.Entry<BlockPos, Kind> entry : assembly.entrySet()) {
                BlockPos partPos = entry.getKey();
                if (level.getBlockEntity(partPos) instanceof PWRPartBlockEntity part) {
                    part.setCorePos(controllerPos);
                }
            }
            controller.setup(assembly);
        }

        controller.setAssembled(!errored[0]);
    }

    private void floodFill(Level level, BlockPos pos, Map<BlockPos, Kind> assembly, Set<BlockPos> fuelRods,
                            Set<BlockPos> sources, boolean[] errored, @Nullable Player player) {
        if (assembly.containsKey(pos) || errored[0]) return;
        if (assembly.size() >= MAX_ASSEMBLY_SIZE) {
            errored[0] = true;
            sendError(level, pos, "Max size exceeded", player);
            return;
        }

        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof PWRPartBlock partBlock)) {
            sendError(level, pos, "Non-reactor block", player);
            errored[0] = true;
            return;
        }

        Kind kind = partBlock.getKind();

        if (kind == Kind.CASING || kind == Kind.REFLECTOR || kind == Kind.PORT) {
            assembly.put(pos, kind);
            return;
        }

        // Core block: fuel/control/channel/heatex/heatsink/neutron_source.
        assembly.put(pos, kind);
        if (kind == Kind.FUEL) fuelRods.add(pos);
        if (kind == Kind.NEUTRON_SOURCE) sources.add(pos);

        floodFill(level, pos.relative(Direction.EAST), assembly, fuelRods, sources, errored, player);
        floodFill(level, pos.relative(Direction.WEST), assembly, fuelRods, sources, errored, player);
        floodFill(level, pos.relative(Direction.UP), assembly, fuelRods, sources, errored, player);
        floodFill(level, pos.relative(Direction.DOWN), assembly, fuelRods, sources, errored, player);
        floodFill(level, pos.relative(Direction.SOUTH), assembly, fuelRods, sources, errored, player);
        floodFill(level, pos.relative(Direction.NORTH), assembly, fuelRods, sources, errored, player);
    }

    /**
     * Substitutes for the original's in-world particle/label marker packet (that exact particle
     * type - a floating text billboard - has no equivalent in this port's particle system): a
     * chat message plus a highlight on the offending block, using the highlight system this port
     * already has (see {@code MultiblockStructureHelper}'s obstruction highlighting).
     */
    public static void sendError(Level level, BlockPos pos, String message, @Nullable Player player) {
        if (player == null) {
            return;
        }
        player.displayClientMessage(Component.literal("[PWR] " + message
                + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"), true);
        if (player instanceof ServerPlayer serverPlayer) {
            com.hbm_m.network.HighlightBlocksPacket.sendTo(serverPlayer, java.util.List.of(pos));
        }
    }
}
