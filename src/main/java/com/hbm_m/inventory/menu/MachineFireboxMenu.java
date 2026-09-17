package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Порт {@code ContainerFirebox} для гритбокса ({@code TileEntityHeaterFirebox}). */
public class MachineFireboxMenu extends FireboxBaseMenu<MachineFireboxBlockEntity> {

    public MachineFireboxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineFireboxMenu(int id, Inventory inventory, MachineFireboxBlockEntity blockEntity) {
        this(id, inventory, blockEntity, blockEntity.getData());
    }

    public MachineFireboxMenu(int id, Inventory inventory, MachineFireboxBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), id, inventory, blockEntity, data);
    }

    private static MachineFireboxBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFireboxBlockEntity fireboxBlockEntity) {
            return fireboxBlockEntity;
        }
        throw new IllegalStateException("No " + RefStrings.MODID + ":firebox block entity at " + pos);
    }
}
