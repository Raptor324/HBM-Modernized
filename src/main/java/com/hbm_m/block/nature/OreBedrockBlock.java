package com.hbm_m.block.nature;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.nature.OreBedrockBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Aequivalent zu {@code BlockBedrockOreTE} aus dem 1.7.10-Original: optisch/physisch identisch
 * mit Bedrock (unzerstoerbar per Hand, siehe {@link #getDestroyProgress}), nur die Maschinen
 * (Mining Drill) koennen es abbauen. Die eigentlichen Erz-/Tier-/Fluid-Daten stecken in der
 * {@link OreBedrockBlockEntity}, gesetzt von {@link com.hbm_m.worldgen.BedrockOreFeature}.
 */
public class OreBedrockBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public OreBedrockBlock(Properties properties) {
        super(properties);
    }

    /**
     * Порт {@code BlockBedrockOreTE.printHook}: имя ресурса (если задан мирогенерацией), тир
     * бура и требуемая кислота. Данные берутся из {@link OreBedrockBlockEntity}, синхронизируются
     * базовым {@code getUpdateTag} (полный NBT).
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.world.level.Level level, BlockPos pos) {
        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof OreBedrockBlockEntity ore)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        if (!ore.resource.isEmpty()) {
            text.add(ore.resource.getHoverName());
        }
        text.add(net.minecraft.network.chat.Component.literal("Tier: " + ore.tier));
        if (ore.acidType != net.minecraft.world.level.material.Fluids.EMPTY && ore.acidAmountMb > 0) {
            text.add(net.minecraft.network.chat.Component.literal("Requires: " + ore.acidAmountMb + "mB ")
                    .append(com.hbm_m.inventory.fluid.FluidType.forFluid(ore.acidType).getLocalizedName()));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreBedrockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<OreBedrockBlock> CODEC = simpleCodec(OreBedrockBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
