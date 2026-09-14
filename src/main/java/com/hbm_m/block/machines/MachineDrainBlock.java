package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineDrainBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code MachineDrain} (1.7.10): der Ablauf.
 *
 * <p>Er belegt wie im Original drei Felder in einer Reihe ({@code getDimensions {0,0,2,0,0,0}}) -
 * der Kern vorn, zwei Dummyzellen dahinter.</p>
 */
public class MachineDrainBlock extends com.hbm_m.block.machines.DummyableMachineBlock {

    public MachineDrainBlock(Properties properties) { super(properties); }

    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        return com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(0, 0, 2, 0, 0, 0)
                .placementOffset(0)
                .build(() -> com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDrainBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_DRAIN_BE.get(),
                (lvl, pos, st, be) -> MachineDrainBlockEntity.tick(lvl, pos, st, (MachineDrainBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide() || !player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof IItemFluidIdentifier identifier)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MachineDrainBlockEntity be)) return InteractionResult.PASS;

        var fluid = identifier.getType(level, pos, held);
        be.retype(fluid);
        player.displayClientMessage(Component.literal("Changed type"), true);
        return InteractionResult.CONSUME;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide() || !player.isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof IItemFluidIdentifier identifier)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof MachineDrainBlockEntity be)) return InteractionResult.PASS;

        var fluid = identifier.getType(level, pos, held);
        be.retype(fluid);
        player.displayClientMessage(Component.literal("Changed type"), true);
        return InteractionResult.CONSUME;
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDrainBlock> CODEC = simpleCodec(MachineDrainBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
