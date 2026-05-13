package com.hbm_m.block.machines;

import com.hbm_m.block.entity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineCrucibleMenu;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;

/**
 * Crucible machine block (single-block, 1.20 port).
 *
 * Legacy equivalent: MachineCrucible (BlockDummyable, 3×3 multi-block).
 * Multi-block support is not ported yet — the crucible is currently a 1×1 block.
 *
 * Shape: hollow bowl — a base plate plus four walls.
 * Shovel interaction and breakBlock content-drops require MaterialStack/ItemScraps
 * to be ported first (marked with TODO below).
 */
public class MachineCrucibleBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Bowl shape: base + N/S/E/W walls
    private static final VoxelShape SHAPE = Shapes.or(
            // Base plate (full width, 4px high)
            Block.box( 0,  0,  0, 16,  4, 16),
            // North wall
            Block.box( 0,  4,  0, 16, 16,  2),
            // South wall
            Block.box( 0,  4, 14, 16, 16, 16),
            // West wall
            Block.box( 0,  4,  0,  2, 16, 16),
            // East wall
            Block.box(14,  4,  0, 16, 16, 16)
    );

    public MachineCrucibleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // -------------------------------------------------------------------------
    // BlockEntity
    // -------------------------------------------------------------------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCrucibleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.CRUCIBLE_BE.get(), MachineCrucibleBlockEntity::serverTick);
    }

    // -------------------------------------------------------------------------
    // Shape / rendering
    // -------------------------------------------------------------------------

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // -------------------------------------------------------------------------
    // Block state
    // -------------------------------------------------------------------------

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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // -------------------------------------------------------------------------
    // Interaction
    // -------------------------------------------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Shovel interaction (legacy parity placeholder):
        // clear current crucible item inventory into player inventory or drop to world.
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof net.minecraft.world.item.ShovelItem) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack extracted = handler.extractItem(i, Integer.MAX_VALUE, false);
                        if (extracted.isEmpty()) continue;

                        if (!player.getInventory().add(extracted.copy())) {
                            Containers.dropItemStack(level,
                                    hit.getLocation().x,
                                    hit.getLocation().y,
                                    hit.getLocation().z,
                                    extracted);
                        }
                    }
                });
                player.inventoryMenu.broadcastChanges();
            }
            return InteractionResult.CONSUME;
        }

        // Normal interaction: open GUI
        if (player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            ContainerData data = (be instanceof MachineCrucibleBlockEntity cbe)
                ? cbe.getData()
                : new SimpleContainerData(4);
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (containerId, playerInventory, p) -> new MachineCrucibleMenu(
                                    containerId,
                                    playerInventory,
                                    be,
                        data
                            ),
                            Component.translatable("container.hbm_m.crucible")
                    ),
                    pos);
        }
        return InteractionResult.CONSUME;
    }

    // -------------------------------------------------------------------------
    // Block removal — drop molten contents as scraps (legacy parity)
    // -------------------------------------------------------------------------

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Drop current crucible inventory on block replacement/break.
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
                        for (int i = 0; i < handler.getSlots(); i++) {
                            ItemStack extracted = handler.extractItem(i, Integer.MAX_VALUE, false);
                            if (!extracted.isEmpty()) {
                                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), extracted);
                            }
                        }
                    });
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}