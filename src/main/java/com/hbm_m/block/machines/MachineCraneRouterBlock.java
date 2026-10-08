package com.hbm_m.block.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code CraneRouter}: nimmt Teile und Pakete von jeder Seite an, sortiert jeden Stapel ueber die sechs Seitenfilter in
 * eine Richtung und legt ihn dort auf das Band (Pakete werden je Richtung neu gepackt) oder wirft ihn ab. Die sechs Seiten
 * tragen farbige Markierungen (rot, orange, gelb, gruen, blau, violett).
 */
public class MachineCraneRouterBlock extends BaseEntityBlock implements IEnterableBlock, IEnterablePackageBlock {

    /** Original: {@code colorMultiplier} der Durchgaenge 1-6 (Seite 0-5). */
    public static final int[] SIDE_COLORS = { 0xff0000, 0xff8000, 0xffff00, 0x00ff00, 0x0080ff, 0x8000ff };

    public MachineCraneRouterBlock(Properties properties) {
        super(properties);
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineCraneRouterBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, com.hbm_m.blockentity.ModBlockEntities.CRANE_ROUTER_BE.get(), MachineCraneRouterBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(level, pos, player, player.getMainHandItem());
    }
    *///?}

    private InteractionResult activate(Level level, BlockPos pos, Player player, ItemStack held) {
        if (!held.isEmpty() && held.getItem() instanceof com.hbm_m.item.tool.ItemConveyorWand) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown()) {
            if (level.getBlockEntity(pos) instanceof MachineCraneRouterBlockEntity router) {
                MenuRegistry.openExtendedMenu((ServerPlayer) player, router, buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorItemEntity entity) { return true; }

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity entity) {
        entity.discard();
        if (!(level.getBlockEntity(pos) instanceof MachineCraneRouterBlockEntity router)) return;
        List<ItemStack>[] sort = router.sort(entity.getItem());

        for (int i = 0; i < 7; i++) {
            List<ItemStack> list = sort[i];
            if (i == 6) {
                for (ItemStack stack : list) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            } else {
                for (ItemStack stack : list) sendOnRoute(level, pos, stack, Direction.from3DDataValue(i));
            }
        }
    }

    protected void sendOnRoute(Level level, BlockPos pos, ItemStack item, Direction dir) {
        BlockPos target = pos.relative(dir);
        if (level.getBlockState(target).getBlock() instanceof IConveyorBelt belt) {
            Vec3 snap = belt.getClosestSnappingPosition(level, target,
                    new Vec3(pos.getX() + 0.5 + dir.getStepX() * 0.55, pos.getY() + 0.5 + dir.getStepY() * 0.55, pos.getZ() + 0.5 + dir.getStepZ() * 0.55));
            level.addFreshEntity(MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, item));
        } else {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5 + dir.getStepX() * 0.55, pos.getY() + 0.5 + dir.getStepY() * 0.55, pos.getZ() + 0.5 + dir.getStepZ() * 0.55, item));
        }
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, MovingConveyorPackageEntity entity) {
        if (!(level.getBlockEntity(pos) instanceof MachineCraneRouterBlockEntity router)) return;
        List<ItemStack>[] sort = router.sort(entity.getContents());

        for (int i = 0; i < 7; i++) {
            List<ItemStack> list = sort[i];
            if (list.isEmpty()) continue;

            if (i == 6) {
                for (ItemStack stack : list) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            } else {
                Direction d = Direction.from3DDataValue(i);
                BlockPos target = pos.relative(d);
                if (level.getBlockState(target).getBlock() instanceof IConveyorBelt belt) {
                    Vec3 snap = belt.getClosestSnappingPosition(level, target,
                            new Vec3(pos.getX() + 0.5 + d.getStepX() * 0.55, pos.getY() + 0.5 + d.getStepY() * 0.55, pos.getZ() + 0.5 + d.getStepZ() * 0.55));
                    level.addFreshEntity(MovingConveyorPackageEntity.create(level, snap.x, snap.y, snap.z, list.toArray(new ItemStack[0])));
                } else {
                    for (ItemStack stack : list) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5 + d.getStepX() * 0.55, pos.getY() + 0.5 + d.getStepY() * 0.55, pos.getZ() + 0.5 + d.getStepZ() * 0.55, stack));
                }
            }
        }
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
    /*public static final com.mojang.serialization.MapCodec<MachineCraneRouterBlock> CODEC = simpleCodec(MachineCraneRouterBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
