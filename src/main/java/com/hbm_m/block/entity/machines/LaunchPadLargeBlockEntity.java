package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Большая пусковая площадка.
 *
 * Сейчас поведение практически идентично обычной LaunchPadBlockEntity,
 * но вынесено в отдельный класс для будущего расширения (анимации,
 * форма ракеты, мультиблок и т.п.).
 *
 * Блок для этой площадки будет добавлен позже, поэтому BlockEntityType
 * временно переиспользует LAUNCH_PAD_BE. Это безопасно до тех пор,
 * пока данный BlockEntity фактически нигде не спавнится в мире.
 */
public class LaunchPadLargeBlockEntity extends LaunchPadBaseBlockEntity {

    public LaunchPadLargeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LaunchPadLargeBlockEntity be) {
        commonServerTick(level, pos, state, be);
    }

    @Override
    protected Component getDefaultName() {
        // В оригинале отдельного ключа не было, поэтому используем свой.
        return Component.translatable("container.launchPadLarge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchPadLargeMenu(containerId, inv, this);
    }

    @Override
    protected boolean isReadyForLaunch() {
        // В будущем здесь можно учесть особенности большой ракеты
        return true;
    }
}
