package com.hbm_m.block.entity.machines;

import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.LaunchPadLargeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Обычная пусковая площадка.
 */
public class LaunchPadBlockEntity extends LaunchPadBaseBlockEntity {

    public LaunchPadBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LAUNCH_PAD_BE.get(), pos, state);
    }

    public LaunchPadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LaunchPadBlockEntity be) {
        if (level.isClientSide) {
            clientLaunchPadSmokeTick(level, pos, state);
        } else {
            commonServerTick(level, pos, state, be);
        }
    }

    @Override
    protected Component getDefaultName() {
        // Сохраняем старый ключ для совместимости с переводами 1.7.10
        return Component.translatable("container.launchPad");
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
        // Кулдаун управляется базой через поле delay (см. commonServerTick).
        return delay <= 0;
    }

    @Override
    public NodeDirPos[] getConPos() {
        return buildStandardConPos(worldPosition);
    }
}
