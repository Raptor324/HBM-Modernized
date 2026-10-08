package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.RadioboxBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code Radiobox}: Wandkasten mit Hebel. Ausrichtung nach Blickrichtung (Original-Metadaten 2-5), Rechtsklick legt
 * den Hebel um (Metadaten +4 = {@link #ON}, Klang {@code reactorStart}), eine Funkenbatterie macht ihn stromunabhaengig
 * und faellt beim Abbau wieder heraus. Gezeichnet vom {@code RadioboxRenderer} ({@code ModelRadio}).
 */
public class RadioboxBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ON = BooleanProperty.create("on");

    private static final VoxelShape WEST = Block.box(11, 1, 4, 16, 15, 12);
    private static final VoxelShape NORTH = Block.box(4, 1, 11, 12, 15, 16);
    private static final VoxelShape EAST = Block.box(0, 1, 4, 5, 15, 12);
    private static final VoxelShape SOUTH = Block.box(4, 1, 0, 12, 15, 5);

    public RadioboxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON);
    }

    /** Original {@code onBlockPlacedBy}: Blick nach Sueden -> Metadatum 2 (Norden) usw. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case WEST -> WEST;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> NORTH;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioboxBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.RADIOBOX_BE.get(),
                (lvl, pos, st, be) -> RadioboxBlockEntity.tick(lvl, pos, st, (RadioboxBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(state, level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(state, level, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult activate(BlockState state, Level world, BlockPos pos, Player player, ItemStack held) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else if (!player.isShiftKeyDown()) {
            if (!(world.getBlockEntity(pos) instanceof RadioboxBlockEntity box)) return InteractionResult.PASS;

            if (!held.isEmpty() && held.getItem() == ModItems.BATTERY_SPARK.get() && !box.infinite) {
                held.shrink(1);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, com.hbm_m.sound.HbmSoundsNT.get("hbm:item.upgradePlug"), SoundSource.BLOCKS, 1.5F, 1.0F);
                box.infinite = true;
                box.setChanged();
                return InteractionResult.CONSUME;
            }

            if (!state.getValue(ON)) {
                world.setBlock(pos, state.setValue(ON, true), 2);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:block.reactorStart"), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                world.setBlock(pos, state.setValue(ON, false), 2);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:block.reactorStart"), SoundSource.BLOCKS, 1.0F, 0.85F);
            }

            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    /** Original {@code breakBlock}: eine eingesetzte Funkenbatterie faellt wieder heraus. */
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !world.isClientSide()
                && world.getBlockEntity(pos) instanceof RadioboxBlockEntity box && box.infinite) {
            world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.BATTERY_SPARK.get())));
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RadioboxBlock> CODEC = simpleCodec(RadioboxBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
