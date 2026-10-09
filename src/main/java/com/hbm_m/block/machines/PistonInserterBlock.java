package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.PistonInserterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code PistonInserter}: Kolben-Einschieber. Nur ueber die Kolbenseite bedienbar: mit Gegenstand legt man einen
 * ein, geschlichen wirft man ihn (eingefahren) wieder aus. Ein Redstone-Flankenwechsel startet den Hub, solange das
 * Feld davor kein Vollblock ist.
 */
public class PistonInserterBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PistonInserterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original {@code BlockPistonBase.determineOrientation}: Kolben zeigt zum Spieler. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PistonInserterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PISTON_INSERTER_BE.get(), PistonInserterBlockEntity::tick);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighbor, BlockPos fromPos, boolean moving) {
        this.updateState(world, pos, state);
    }

    protected void updateState(Level world, BlockPos pos, BlockState state) {
        if (!world.isClientSide) {
            Direction dir = state.getValue(FACING);
            BlockPos front = pos.relative(dir);

            if (world.getBlockState(front).isRedstoneConductor(world, front))
                return; //no obstructions allowed!

            boolean flag = world.hasNeighborSignal(pos);
            if (!(world.getBlockEntity(pos) instanceof PistonInserterBlockEntity piston)) return;

            if (flag && !piston.lastState && piston.extend <= 0)
                piston.isRetracting = false;

            piston.lastState = flag;
            piston.setChanged();
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}

        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof PistonInserterBlockEntity piston) {

                if (!piston.slot.isEmpty() && piston.isRetracting) {
                    Direction dir = state.getValue(FACING);

                    ItemEntity dust = new ItemEntity(world, pos.getX() + 0.5D + dir.getStepX() * 0.75D, pos.getY() + 0.5D + dir.getStepY() * 0.75D, pos.getZ() + 0.5D + dir.getStepZ() * 0.75D, piston.slot);
                    piston.slot = ItemStack.EMPTY;

                    dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
                    world.addFreshEntity(dust);
                    piston.markForSync();
                }
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        } else if (!player.getItemInHand(hand).isEmpty()) {
            if (!world.isClientSide && world.getBlockEntity(pos) instanceof PistonInserterBlockEntity piston) {

                if (piston.slot.isEmpty()) {
                    piston.slot = player.getItemInHand(hand).split(1);
                    player.inventoryMenu.broadcastChanges();
                    piston.markForSync();
                }
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && world.getBlockEntity(pos) instanceof PistonInserterBlockEntity piston && !piston.slot.isEmpty()) {
            Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), piston.slot);
        }
        super.onRemove(state, world, pos, newState, moving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<PistonInserterBlock> CODEC = simpleCodec(PistonInserterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
