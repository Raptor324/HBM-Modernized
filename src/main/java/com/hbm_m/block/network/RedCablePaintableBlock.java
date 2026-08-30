package com.hbm_m.block.network;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.network.RedCablePaintableBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Порт BlockCablePaintable (1.7.10): цельноблочный кабель-камуфляж.
 * ПКМ любым блоком окрашивается под него (allowedPaint 1:1), сброс — отвёрткой в оригинале,
 * здесь — ПКМ пустой рукой при Shift. Рендер замаскированного блока — RedCablePaintableRenderer.
 */
public class RedCablePaintableBlock extends BaseEntityBlock {

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RedCablePaintableBlock> CODEC = simpleCodec(RedCablePaintableBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    public RedCablePaintableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedCablePaintableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof RedCablePaintableBlockEntity paintable) {
                RedCablePaintableBlockEntity.tick(lvl, pos, st, paintable);
            }
        };
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(state, level, pos, player, player.getUsedItemHand());
    }
    *///?}

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.getBlockEntity(pos) instanceof RedCablePaintableBlockEntity paintable) {
            ItemStack held = player.getItemInHand(hand);

            if (player.isShiftKeyDown() && held.isEmpty() && paintable.getCamo() != null) {
                if (!level.isClientSide) paintable.setCamo(null);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (!held.isEmpty() && held.getItem() instanceof BlockItem blockItem) {
                Block paint = blockItem.getBlock();
                BlockState paintState = paint.defaultBlockState();
                if (allowedPaint(level, pos, paint, paintState)) {
                    if (!level.isClientSide) paintable.setCamo(paintState);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    /** Порт allowedPaint: красить можно любым «нормально рендерящимся» блоком, кроме самого кабеля и травы. */
    private static boolean allowedPaint(Level level, BlockPos pos, Block paint, BlockState paintState) {
        if (paint == net.minecraft.world.level.block.Blocks.GRASS_BLOCK) return false;
        if (paint == com.hbm_m.block.ModBlocks.RED_CABLE_PAINTABLE.get()) return false;
        return paintState.isSolidRender(level, pos);
    }

    private void addTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.hbm_m.paintable").withStyle(ChatFormatting.GRAY));
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
