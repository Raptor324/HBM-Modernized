package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.LaunchPadRustedMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ржавая пусковая площадка (Launch Pad Rusted).
 *
 * Как и у обычной площадки, ракетная логика пока заглушена.
 * Отличается только типом BlockEntity и используемым меню/GUI.
 */
public class LaunchPadRustedBlockEntity extends LaunchPadBaseBlockEntity {

    public LaunchPadRustedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAUNCH_PAD_RUSTED_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LaunchPadRustedBlockEntity be) {
        commonServerTick(level, pos, state, be);
    }

    @Override
    protected Component getDefaultName() {
        // В оригинале использовался отдельный GUI, поэтому даём отдельный ключ
        return Component.translatable("container.launchPadRusted");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new LaunchPadRustedMenu(containerId, inv, this);
    }

    @Override
    protected boolean isReadyForLaunch() {
        // Ржавая площадка может требовать отдельных условий; пока считаем, что готова
        return true;
    }
}

