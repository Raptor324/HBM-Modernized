package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineChimneyBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code MachineChimneyBrick} ({@code getDimensions {12,0,1,1,1,1}}) und {@code MachineChimneyIndustrial}
 * ({@code {22,0,1,1,1,1}}), beide {@code getOffset 1}; die vier Seitenzellen des Sockels nehmen Rauch an. Gezeichnet
 * vom {@code ChimneyRenderer}.
 */
public class MachineChimneyBlock extends DummyableMachineBlock {

    /** defineStructure laeuft im Superkonstruktor, bevor {@code height} gesetzt ist. */
    private static final ThreadLocal<Integer> PENDING_HEIGHT = ThreadLocal.withInitial(() -> 12);

    private final int height;

    private static Properties stash(Properties properties, int height) {
        PENDING_HEIGHT.set(height);
        return properties;
    }

    public MachineChimneyBlock(Properties properties, int height) {
        super(stash(properties, height));
        this.height = height;
    }

    public int getHeight() { return height; }

    public boolean isIndustrial() { return height > 12; }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(PENDING_HEIGHT.get(), 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineChimneyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CHIMNEY_BE.get(),
                (lvl, pos, st, be) -> MachineChimneyBlockEntity.tick(lvl, pos, st, (MachineChimneyBlockEntity) be));
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
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(p -> new MachineChimneyBlock(p, this.height));
    }
    *///?}
}
