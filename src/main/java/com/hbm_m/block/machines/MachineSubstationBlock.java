package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSubstationBlockEntity;
import com.hbm_m.blockentity.network.PylonBaseBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code Substation} (1.7.10): Umspannwerk als Dummyable-Pylon. {@code getDimensions {4,0,1,1,2,2}},
 * Setzversatz 1, vier Eck-Anschluesse ({@code makeExtra} bei Kern +-1/+-1). Rechtsklick faerbt das Kabel
 * ({@code setColor}), keine GUI.
 */
public class MachineSubstationBlock extends DummyableMachineBlock {

    public MachineSubstationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(4, 0, 1, 1, 2, 2)
                .extra(1, 0, 1)
                .extra(1, 0, -1)
                .extra(-1, 0, 1)
                .extra(-1, 0, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSubstationBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SUBSTATION_BE.get(),
                (lvl, pos, st, be) -> PylonBaseBlockEntity.tick(lvl, pos, st, be));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Original breakBlock: disconnectAll vor dem Abbau
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
            pylon.disconnectAll();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return interact(level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof PylonBaseBlockEntity pylon) {
            return pylon.setColor(player.getItemInHand(hand)) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    /** Original {@code addInformation}: Verbindungsart und Reichweite. */
    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        addInfo(tooltip);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addInfo(tooltip);
    }
    *///?}

    private static void addInfo(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.hbm_m.connection_type")
                .append(Component.translatable("tooltip.hbm_m.connection_quad").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hbm_m.connection_range")
                .append(Component.literal("20m").withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSubstationBlock> CODEC = simpleCodec(MachineSubstationBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
