package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
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
 * Port of the 1.7.10 FoundryTank block. Right-click with a shovel empties its contents into a
 * {@code scraps} item stack (simplified: plain count-based stack, no material/amount NBT encoding
 * like the original {@code ItemScraps} - documented simplification, matches the "reasonable but not
 * pixel-perfect" precedent used elsewhere in this port for byproduct-recovery items).
 */
public class MachineFoundryTankBlock extends BaseEntityBlock {

    public MachineFoundryTankBlock(Properties props) { super(props); }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineFoundryTankBlockEntity tank)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ShovelItem && tank.amount > 0) {
            int scrapCount = Math.max(1, Math.min(64, tank.amount / com.hbm_m.inventory.material.MaterialStack.MB_PER_INGOT));
            ItemStack scrap = new ItemStack(ModItems.SCRAPS.get(), scrapCount);
            if (!player.addItem(scrap)) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, scrap));
            }
            tank.amount = 0;
            tank.type = null;
            tank.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineFoundryTankBlockEntity tank && tank.amount > 0) {
            int scrapCount = Math.max(1, Math.min(64, tank.amount / com.hbm_m.inventory.material.MaterialStack.MB_PER_INGOT));
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    new ItemStack(ModItems.SCRAPS.get(), scrapCount)));
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineFoundryTankBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_TANK_BE.get(),
                MachineFoundryTankBlockEntity::tick);
    }
}
