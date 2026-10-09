package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.RefuelerBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.FluidType;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockRefueler}: wandmontierte Betankungsstation (4 Pixel tief an der Rueckwand). Mit Fluessigkeitskennung
 * wird der Tanktyp umgestellt ("Changed type to ...!").
 */
public class RefuelerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH = Block.box(0, 0, 12, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 4);
    private static final VoxelShape WEST = Block.box(12, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 4, 16, 16);

    public RefuelerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original {@code onBlockPlacedBy}: Front zum Spieler. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> net.minecraft.world.phys.shapes.Shapes.block();
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefuelerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.REFUELER_BE.get(), RefuelerBlockEntity::tick);
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
        if (!world.isClientSide && !player.isShiftKeyDown()) {
            ItemStack held = player.getItemInHand(hand);
            if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier id) {

                if (!(world.getBlockEntity(pos) instanceof RefuelerBlockEntity refueler))
                    return InteractionResult.FAIL;

                Fluid type = id.getType(world, pos, held);
                refueler.tank.setTankType(type);
                refueler.setChanged();
                world.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(FluidType.forFluid(type).getLocalizedName()).append(Component.literal("!")));

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RefuelerBlock> CODEC = simpleCodec(RefuelerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
