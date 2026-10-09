package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.generic.WandStructureBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockWandStructure} (wand_structure): Meta 0 = Speicherblock ({@code wand_structure_save}), Meta 1 =
 * Ladeblock ({@code wand_structure_load}). Rechtsklick oeffnet die GUI; mit einem Block in der Hand wird dieser in
 * die Ausschlussliste aufgenommen bzw. daraus entfernt.
 */
public class BlockWandStructure extends BaseEntityBlock implements ILookOverlay {

    public final boolean load;

    public BlockWandStructure(Properties properties, boolean load) {
        super(properties);
        this.load = load;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandStructureBlockEntity(pos, state);
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
        return onActivated(world, pos, player, hand);
    }

    private InteractionResult onActivated(Level world, BlockPos pos, Player player, InteractionHand hand) {
        if (!(world.getBlockEntity(pos) instanceof WandStructureBlockEntity structure)) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) {
            ItemStack held = player.getItemInHand(hand);
            Block block = held.getItem() instanceof BlockItem bi ? bi.getBlock() : null;
            if (block != null && !isStructureBlock(block)) {
                BlockState bm = block.defaultBlockState();

                if (!world.isClientSide) {
                    if (structure.blacklist.contains(bm)) {
                        structure.blacklist.remove(bm);
                    } else {
                        structure.blacklist.add(bm);
                    }
                    structure.sync();
                }

                return InteractionResult.sidedSuccess(world.isClientSide);
            }

            if (world.isClientSide) {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                        com.hbm_m.client.gui.structure.StructureScreenOpener.open(pos, load));
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /** Original {@code ModBlocks.isStructureBlock(block, true)}. */
    public static boolean isStructureBlock(Block block) {
        if (block == com.hbm_m.block.ModBlocks.WAND_AIR.get()) return true;
        if (block instanceof BlockWandStructure) return true;
        if (block == com.hbm_m.block.ModBlocks.WAND_JIGSAW.get()) return true;
        if (block instanceof BlockWandLogic) return true;
        if (block == com.hbm_m.block.ModBlocks.WAND_TANDEM.get()) return true;
        if (block == com.hbm_m.block.ModBlocks.WAND_LOOT.get()) return true;
        return false;
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {
        if (load) return;

        if (!(world.getBlockEntity(pos) instanceof WandStructureBlockEntity structure)) return;

        List<Component> text = new ArrayList<>();

        text.add(Component.literal("Name: ").withStyle(ChatFormatting.GRAY).append(Component.literal(structure.name).withStyle(ChatFormatting.WHITE)));

        text.add(Component.literal("Blacklist:").withStyle(ChatFormatting.GRAY));
        for (BlockState bm : structure.blacklist) {
            text.add(Component.literal("- " + bm.getBlock().getDescriptionId() + " : " + bm).withStyle(ChatFormatting.RED));
        }

        ILookOverlay.printGeneric(guiGraphics, getName(), 0xffff00, 0x404000, text);
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockWandStructure> CODEC = com.hbm_m.platform.BlockCodecs.unsupported(BlockWandStructure.class);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
