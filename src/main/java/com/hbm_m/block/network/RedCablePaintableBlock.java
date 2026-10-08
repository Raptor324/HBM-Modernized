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
public class RedCablePaintableBlock extends BaseEntityBlock implements com.hbm_m.api.block.IToolable {

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RedCablePaintableBlock> CODEC = simpleCodec(RedCablePaintableBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    /**
     * Original Metadate 0 ({@code true}): im zweiten Renderdurchgang liegt ueber dem Anstrich das Kabel-Overlay;
     * Metadate 1 ({@code false}): der Anstrich bleibt ohne Overlay. Der Entschaerfer schaltet um.
     */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty OVERLAY =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("overlay");

    public RedCablePaintableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OVERLAY, true));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OVERLAY);
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

            // audit10: Original - nur ein ungestrichenes Kabel laesst sich anstreichen; Entfernen per Schraubenzieher (onScrew)
            if (!held.isEmpty() && held.getItem() instanceof BlockItem blockItem && paintable.getCamo() == null) {
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

    /**
     * audit10: 1:1 {@code BlockCablePaintable.onScrew} - der Schraubenzieher entfernt den Anstrich, der Entschaerfer
     * schaltet Metadate 0/1 ({@link #OVERLAY}) um - auch am ungestrichenen Kabel, dort ohne sichtbare Wirkung.
     */
    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, net.minecraft.core.Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool == ToolType.SCREWDRIVER && world.getBlockEntity(pos) instanceof RedCablePaintableBlockEntity pipe && pipe.getCamo() != null) {
            if (!world.isClientSide) pipe.setCamo(null);
            return true;
        }

        if (tool == ToolType.DEFUSER) {
            BlockState state = world.getBlockState(pos);
            if (state.hasProperty(OVERLAY)) world.setBlock(pos, state.setValue(OVERLAY, !state.getValue(OVERLAY)), 3);
            return true;
        }
        return false;
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
