package com.hbm_m.block.bomb;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.bomb.FireworksBlockEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockFireworks} ("Firework Battery"): Schwarzpulver (x3) oder Schwefel laden den ganzen Stapel als
 * Ladungen, Farbstoff setzt die Farbe, ein Namensschild die Nachricht; sonst zeigt Rechtsklick den Zustand.
 */
public class FireworksBlock extends BaseEntityBlock {

    public FireworksBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireworksBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FIREWORKS.get(), FireworksBlockEntity::serverTick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(world, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(world, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof FireworksBlockEntity te)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty()) {
            if (held.is(Items.GUNPOWDER)) {
                te.charges += held.getCount() * 3;
                te.setChanged();
                held.setCount(0);
                return InteractionResult.SUCCESS;
            }
            if (held.is(ModItems.SULFUR.get())) {
                te.charges += held.getCount();
                te.setChanged();
                held.setCount(0);
                return InteractionResult.SUCCESS;
            }
            if (held.getItem() instanceof DyeItem dye) {
                te.color = dye.getDyeColor().getFireworkColor();
                te.setChanged();
                held.shrink(1);
                return InteractionResult.SUCCESS;
            }
            if (held.is(Items.NAME_TAG)) {
                te.message = held.getHoverName().getString();
                te.setChanged();
                held.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }

        player.sendSystemMessage(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("block.hbm_m.fireworks.charges", te.charges).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("block.hbm_m.fireworks.color", Integer.toHexString(te.color)).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("block.hbm_m.fireworks.message", te.message).withStyle(ChatFormatting.YELLOW));
        return InteractionResult.SUCCESS;
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<FireworksBlock> CODEC = simpleCodec(FireworksBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
