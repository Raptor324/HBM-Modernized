package com.hbm_m.block;

import net.minecraft.ChatFormatting;
// import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
// import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
// import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// import com.hbm_m.radiation.ChunkRadiationManager;

import java.util.List;

public class RadioactiveBlock extends Block {

    private final float radiationLevel;

    public RadioactiveBlock(Properties properties, float radiationLevel) {
        super(properties);
        this.radiationLevel = radiationLevel;
    }

    public float getRadiationLevel() {
        return radiationLevel;
    }

    @Override
    public void appendHoverText(@javax.annotation.Nonnull ItemStack stack, @Nullable BlockGetter level, @Nullable List<Component> tooltip, @Nonnull TooltipFlag flag) {
        
        if (tooltip != null) {
            super.appendHoverText(stack, level, tooltip, flag);
            tooltip.add(Component.translatable("item.hbm_m.radioactive").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal(radiationLevel + "RAD/s").withStyle(ChatFormatting.YELLOW));

            // Суммарная радиоактивность = уровень * количество блоков в стаке
            int count = stack.getCount();
            float totalRadiation = radiationLevel * count;

            // Добавляем информацию о радиоактивности в подсказку
            
            if (count > 1) {
                tooltip.add(Component.literal("Stack: " + totalRadiation + " RAD/s").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    // @Override
    // @SuppressWarnings("deprecation")
    // public void onRemove(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos, @Nonnull BlockState pNewState, boolean pIsMoving) {
    //     // We only care about server-side, and if the block was actually removed (not just moved by a piston)
    //     if (!pLevel.isClientSide() && !pIsMoving) {
    //         // We also check if the new block is the same as the old one. If it is, we do nothing.
    //         // This prevents recalculation on simple block updates.
    //         if (pNewState.getBlock() != this) {
    //             // Use the manager's proxy to trigger an update for the chunk this block was in.
    //             ChunkRadiationManager.getProxy().onBlockUpdated(pLevel, pPos);
    //         }
    //     }
    //     super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    // }
    
    // // It's also better practice to handle placement here too for consistency
    // @Override
    // @SuppressWarnings("deprecation")
    // public void onPlace(@Nonnull BlockState pState, @Nonnull Level pLevel, @Nonnull BlockPos pPos, @Nonnull BlockState pOldState, boolean pIsMoving) {
    //     super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    //     if (!pLevel.isClientSide()) {
    //         // If we are replacing a different block type, trigger an update.
    //         if (pOldState.getBlock() != this) {
    //              ChunkRadiationManager.getProxy().onBlockUpdated(pLevel, pPos);
    //         }
    //     }
    // }
}