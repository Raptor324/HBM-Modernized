package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSatLinkBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.item.ISatChip;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code com.hbm.blocks.machine.MachineSatLink} ({@code machine_satlink}, Satelliten-Bodenstation): 2x2 Felder,
 * 7 hoch ({@code getDimensions {6,0,1,0,1,0}}), drei Anschlusszellen fuer Funkfackeln. Rechtsklick mit einem
 * Satellitenchip uebernimmt dessen Frequenz.
 */
public class MachineSatLinkBlock extends DummyableMachineBlock implements ILookOverlay {

    public MachineSatLinkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: getDimensions {6,0,1,0,1,0}, getOffset 0, makeExtra -dir, +rot, -dir+rot
        return DummyableStructureBuilder.create()
                .box(6, 0, 1, 0, 1, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(-1, 0, 1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSatLinkBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SATLINK_BE.get(), MachineSatLinkBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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

            if (!held.isEmpty() && held.getItem() instanceof ISatChip) {
                if (!(world.getBlockEntity(pos) instanceof MachineSatLinkBlockEntity link)) return InteractionResult.PASS;

                link.freq = ISatChip.getFreqS(held);
                link.setChanged();
                player.sendSystemMessage(Component.literal("Set frequency to " + link.freq).withStyle(ChatFormatting.YELLOW));
                world.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TOOL_TECH_BLEEP.get(), SoundSource.PLAYERS, 1F, 1F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiblockPart part && part.getControllerPos() != null) be = level.getBlockEntity(part.getControllerPos());
        if (!(be instanceof MachineSatLinkBlockEntity link)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Freq: " + link.freq));
        text.add(Component.literal("Connected: " + (link.connected ? (ChatFormatting.GREEN + "Yes") : (ChatFormatting.RED + "No"))));
        ILookOverlay.printGeneric(guiGraphics, Component.translatable(this.getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSatLinkBlock> CODEC = simpleCodec(MachineSatLinkBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
