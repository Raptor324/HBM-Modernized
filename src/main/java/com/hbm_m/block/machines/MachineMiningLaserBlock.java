package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
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
 * 1:1 {@code MachineMiningLaser}: {@code getDimensions {1,1,1,1,1,1}}, {@code getOffset 0}, {@code getHeightOffset -1}
 * (der Kern sitzt eine Ebene unter dem Klickpunkt, daher nur an Decken platzierbar). Anschlusszellen an den vier
 * Seiten und oben. Gezeichnet vom {@code MiningLaserRenderer}.
 */
public class MachineMiningLaserBlock extends DummyableMachineBlock {

    public MachineMiningLaserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(1, 1, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .extra(0, 1, 0)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return -1;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineMiningLaserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MINING_LASER_BE.get(),
                (lvl, pos, st, be) -> MachineMiningLaserBlockEntity.tick(lvl, pos, st, (MachineMiningLaserBlockEntity) be));
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
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachineMiningLaserBlockEntity laser) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, laser, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.translatable("block.hbm_m.mining_laser.desc.multiblock"));
        list.add(Component.translatable("block.hbm_m.mining_laser.desc.ceiling"));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineMiningLaserBlock> CODEC = simpleCodec(MachineMiningLaserBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
