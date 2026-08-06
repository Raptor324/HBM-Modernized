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

/** Port of {@code MachineDrain} (1.7.10 Original). */
public class MachineDrainBlock extends BaseEntityBlock {

    public MachineDrainBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDrainBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_DRAIN_BE.get(),
                (lvl, pos, st, be) -> MachineDrainBlockEntity.tick(lvl, pos, st, (MachineDrainBlockEntity) be));
    }

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
}
