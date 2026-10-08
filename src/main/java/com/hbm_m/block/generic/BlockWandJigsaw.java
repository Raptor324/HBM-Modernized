package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.generic.WandJigsawBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockWandJigsaw} (wand_jigsaw): Anschlusspunkt fuer Jigsaw-Strukturen. Ausrichtung wie ein Kolben
 * (zum Spieler), ein Block in der Hand wird zum Ersatzblock (wand_air = Luft), sonst oeffnet sich die Einstellungs-GUI.
 * Beim Strukturaufbau wird er durch den Ersatzblock ersetzt (ausser mit Struktur-Debug).
 */
public class BlockWandJigsaw extends BaseEntityBlock implements ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public BlockWandJigsaw(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original {@code BlockPistonBase.determineOrientation}: die Vorderseite zeigt zum Spieler. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandJigsawBlockEntity(pos, state);
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
        if (!(world.getBlockEntity(pos) instanceof WandJigsawBlockEntity jigsaw)) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) {
            ItemStack held = player.getItemInHand(hand);
            Block block = held.getItem() instanceof BlockItem bi ? bi.getBlock() : null;
            if (block == ModBlocks.WAND_AIR.get()) block = Blocks.AIR;

            if (block != null && !BlockWandStructure.isStructureBlock(block)) {
                if (!world.isClientSide) {
                    jigsaw.replaceBlock = block.defaultBlockState();
                    jigsaw.sync();
                }
                return InteractionResult.sidedSuccess(world.isClientSide);
            }

            if (!held.isEmpty() && BuiltInRegistries.ITEM.getKey(held.getItem()).getPath().equals("wand_s")) return InteractionResult.PASS;

            if (world.isClientSide) {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                        com.hbm_m.client.gui.structure.WandScreenOpener.openJigsaw(pos));
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof WandJigsawBlockEntity jigsaw)) return;

        List<Component> text = new ArrayList<>();

        text.add(Component.literal("Target pool: ").withStyle(ChatFormatting.GRAY).append(Component.literal(jigsaw.pool).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        text.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY).append(Component.literal(jigsaw.name).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        text.add(Component.literal("Target name: ").withStyle(ChatFormatting.GRAY).append(Component.literal(jigsaw.target).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        text.add(Component.literal("Turns into: ").withStyle(ChatFormatting.GRAY).append(Component.literal(BuiltInRegistries.BLOCK.getKey(jigsaw.replaceBlock.getBlock()).toString()).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        text.add(Component.literal("   with state: ").withStyle(ChatFormatting.GRAY).append(Component.literal(jigsaw.replaceBlock.toString()).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        text.add(Component.literal("Selection/Placement priority: ").withStyle(ChatFormatting.GRAY).append(Component.literal(jigsaw.selectionPriority + "/" + jigsaw.placementPriority).withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        text.add(Component.literal("Joint type: ").withStyle(ChatFormatting.GRAY).append(Component.literal(jigsaw.isRollable ? "Rollable" : "Aligned").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));

        ILookOverlay.printGeneric(guiGraphics, getName(), 0xffff00, 0x404000, text);
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockWandJigsaw> CODEC = simpleCodec(BlockWandJigsaw::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
