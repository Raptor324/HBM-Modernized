package com.hbm_m.block.decorations;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.block.IToolable;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.PartEmitterBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code PartEmitter} ({@code part_emitter}): Partikelquelle, der Handbohrer schaltet die Effekte durch. */
public class PartEmitterBlock extends BaseEntityBlock implements IToolable {

    public PartEmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PartEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PART_EMITTER.get(), PartEmitterBlockEntity::serverTick);
    }

    @Override
    public boolean onScrew(Level world, Player player, BlockPos pos, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool == ToolType.HAND_DRILL && world.getBlockEntity(pos) instanceof PartEmitterBlockEntity te) {
            te.effect = (te.effect + 1) % PartEmitterBlockEntity.EFFECT_COUNT;
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
        tooltip.add(Component.translatable("tooltip.hbm_m.emitter.hand_drill").withStyle(ChatFormatting.GOLD));
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<PartEmitterBlock> CODEC = simpleCodec(PartEmitterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
