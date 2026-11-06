package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ContainerFirebox} для Heating Oven ({@code TileEntityHeaterOven}) —
 * раскладка и логика общая с гритбоксом, отличается только тип.
 */
public class HeatingOvenMenu extends FireboxBaseMenu<HeatingOvenBlockEntity> {

    private final Level level;

    public HeatingOvenMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, resolve(inv, extraData));
    }

    public HeatingOvenMenu(int containerId, Inventory inv, BlockEntity entity) {
        this(containerId, inv, require(entity), ((HeatingOvenBlockEntity) entity).getData());
    }

    public HeatingOvenMenu(int containerId, Inventory inv, HeatingOvenBlockEntity be, ContainerData data) {
        super(ModMenuTypes.HEATING_OVEN_MENU.get(), containerId, inv, be, data);
        this.level = inv.player.level();
    }

    private static HeatingOvenBlockEntity resolve(Inventory inv, FriendlyByteBuf extraData) {
        return require(inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    private static HeatingOvenBlockEntity require(BlockEntity entity) {
        if (entity instanceof HeatingOvenBlockEntity oven) {
            return oven;
        }
        throw new IllegalStateException("Expected HeatingOvenBlockEntity, got: " + entity);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.HEATING_OVEN.get());
    }
}
