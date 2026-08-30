package com.hbm_m.block.network;

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.network.PylonBaseBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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

/**
 * Базовое ядро пилона ЛЭП — порт BlockDummyable-пилонов 1.7.10 (PylonRedWire/PylonMedium/PylonLarge).
 * При установке заполняет габарит dims{up,down,mx,px,mz,pz} фиктивными частями
 * ({@link PylonDummyBlock}); ПКМ красителем окрашивает кабель (setColor оригинала).
 */
public abstract class RedPylonCoreBlock extends BaseEntityBlock {

    /** {up, down, minusX, plusX, minusZ, plusZ} — габарит структуры относительно ядра. */
    protected abstract int[] getDimensions();

    protected abstract BlockEntityType<? extends PylonBaseBlockEntity> getBlockEntityType();

    protected abstract String getTypeKey();
    protected abstract int getRange();

    /** Хелпер codec() для 1.21.1: лямбда захватывает параметры подкласса (флаг трансформера и т.п.). */
    //? if > 1.20.1 {
    /*protected <T extends RedPylonCoreBlock> com.mojang.serialization.MapCodec<T> makeCodec(Function<Properties, T> factory) {
        return simpleCodec(factory::apply);
    }
    *///?}

    public static final DirectionProperty FACING = DirectionProperty.create("facing",
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);

    protected RedPylonCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return getBlockEntityType().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof PylonBaseBlockEntity pylon) {
                PylonBaseBlockEntity.tick(lvl, pos, st, pylon);
            }
        };
    }

    // ══════════════════════ Фиктивные части ══════════════════════

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.getBlock() == this) return;
        fillDummies(level, pos);
    }

    private void fillDummies(Level level, BlockPos pos) {
        int[] d = getDimensions();
        BlockState dummy = ModBlocks.PYLON_DUMMY.get().defaultBlockState();
        for (int y = -d[1]; y <= d[0]; y++) {
            for (int x = -d[2]; x <= d[3]; x++) {
                for (int z = -d[4]; z <= d[5]; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos p = pos.offset(x, y, z);
                    if (level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, dummy, Block.UPDATE_CLIENTS);
                        if (level.getBlockEntity(p) instanceof com.hbm_m.blockentity.network.PylonDummyBlockEntity be) {
                            be.setCorePos(pos);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && newState.getBlock() != this) {
            clearDummies(level, pos);
            if (level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
                pylon.disconnectAll();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void clearDummies(Level level, BlockPos pos) {
        int[] d = getDimensions();
        for (int y = -d[1]; y <= d[0]; y++) {
            for (int x = -d[2]; x <= d[3]; x++) {
                for (int z = -d[4]; z <= d[5]; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    BlockPos p = pos.offset(x, y, z);
                    if (level.getBlockState(p).getBlock() instanceof PylonDummyBlock) {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
    }

    // ══════════════════════ Окраска красителем ══════════════════════

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
            ItemStack held = player.getItemInHand(hand);
            if (pylon.setColor(held)) {
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private void addTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.hbm_m.connection_type")
                .append(Component.translatable(getTypeKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.connection_range")
                .append(Component.literal(getRange() + "m").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(tooltip);
    }
    *///?}
}
