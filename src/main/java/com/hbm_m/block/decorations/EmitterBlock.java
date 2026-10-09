package com.hbm_m.block.decorations;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.EmitterBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

/**
 * 1:1 {@code BlockEmitter} ({@code deco_emitter}): Lichtstrahl bis 100 Bloecke in Blickrichtung ({@code
 * BlockPistonBase.determineOrientation}). Farbstoff faerbt, Schraubenzieher/Defuser machen den Strahl breiter/
 * schmaler, Handbohrer schaltet die Effekte durch.
 */
public class EmitterBlock extends BaseEntityBlock implements IToolable {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public EmitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** {@code BlockPistonBase.determineOrientation}: nah am Block ueber/unter dem Spieler senkrecht, sonst zum Spieler hin. */
    public static Direction determineOrientation(BlockPos pos, @Nullable Player player) {
        if (player == null) return Direction.NORTH;
        if (Mth.abs((float) player.getX() - pos.getX()) < 2.0F && Mth.abs((float) player.getZ() - pos.getZ()) < 2.0F) {
            double eye = player.getY() + 1.82D;
            if (eye - pos.getY() > 2.0D) return Direction.UP;
            if (pos.getY() - eye > 0.0D) return Direction.DOWN;
        }
        int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (l) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, determineOrientation(ctx.getClickedPos(), ctx.getPlayer()));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DECO_EMITTER.get(), EmitterBlockEntity::serverTick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return hbmOnUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    /** {@code onBlockActivated}: Farbstoff setzt die Farbe ({@code ItemDye.field_150922_c} = Feuerwerksfarbe) und wird verbraucht. */
    private InteractionResult hbmOnUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof DyeItem dye && level.getBlockEntity(pos) instanceof EmitterBlockEntity te) {
            te.color = dye.getDyeColor().getFireworkColor();
            te.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            held.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (!(world.getBlockEntity(pos) instanceof EmitterBlockEntity te)) return false;

        if (tool == ToolType.SCREWDRIVER) {
            te.girth += 0.125F;
            te.setChanged();
            return true;
        }
        if (tool == ToolType.DEFUSER) {
            te.girth -= 0.125F;
            if (te.girth < 0.125F) te.girth = 0.125F;
            te.setChanged();
            return true;
        }
        if (tool == ToolType.HAND_DRILL) {
            te.effect = (te.effect + 1) % EmitterBlockEntity.EFFECT_COUNT;
            te.setChanged();
            return true;
        }
        return false;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> tooltip, TooltipFlag flag) {
    *///?}
        tooltip.add(Component.translatable("tooltip.hbm_m.deco_emitter.screwdriver").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.deco_emitter.defuser").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.emitter.hand_drill").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.deco_emitter.dye").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<EmitterBlock> CODEC = simpleCodec(EmitterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
