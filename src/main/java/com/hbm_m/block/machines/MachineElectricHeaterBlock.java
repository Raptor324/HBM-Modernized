package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineElectricHeaterBlockEntity;
import com.hbm_m.item.ModItems;

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

/** Port of {@code HeaterElectric} (1.7.10 Original). */
public class MachineElectricHeaterBlock extends BaseEntityBlock {

    public MachineElectricHeaterBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineElectricHeaterBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.ELECTRIC_HEATER_BE.get(),
                (lvl, pos, st, be) -> MachineElectricHeaterBlockEntity.tick(lvl, pos, st, (MachineElectricHeaterBlockEntity) be));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == ModItems.SCREWDRIVER.get()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineElectricHeaterBlockEntity be) {
                be.cycleSetting();
                player.displayClientMessage(Component.literal("Setting: " + be.getSetting()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }
}
