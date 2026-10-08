package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCombinationOvenBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code FurnaceCombination}: {@code getDimensions {1,0,1,1,1,1}}, {@code getOffset 1}; jede Zelle ist Inventar-
 * und Fluidzugang ({@code TileEntityProxyCombo().inventory().fluid()}). Gezeichnet vom {@code CombinationOvenRenderer}.
 */
public class MachineCombinationOvenBlock extends DummyableMachineBlock {

    public MachineCombinationOvenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(1, 0, 1, 1, 1, 1);
        for (int up = 0; up <= 1; up++)
            for (int f = -1; f <= 1; f++)
                for (int s = -1; s <= 1; s++)
                    if (up != 0 || f != 0 || s != 0) b = b.extra(f, up, s);
        return b.placementOffset(1).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCombinationOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.COMBINATION_OVEN_BE.get(),
                (lvl, pos, st, be) -> MachineCombinationOvenBlockEntity.tick(lvl, pos, st, (MachineCombinationOvenBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineCombinationOvenBlockEntity machine) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, machine, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCombinationOvenBlock> CODEC = simpleCodec(MachineCombinationOvenBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
