package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Direct clone of {@link MachineAdvancedAssemblerMenu} for the Precision Assembler - identical
 * slot layout/logic, just registered under its own {@link ModMenuTypes#MACHINE_PRECASS_MENU} and
 * validated against {@link ModBlocks#MACHINE_PRECASS}.
 */
public class MachinePrecAssMenu extends MachineAdvancedAssemblerMenu {

    public MachinePrecAssMenu(int pContainerId, Inventory pPlayerInventory, FriendlyByteBuf extraData) {
        this(pContainerId, pPlayerInventory, getBlockEntity(pPlayerInventory, extraData), new SimpleContainerData(2));
    }

    public MachinePrecAssMenu(int pContainerId, Inventory pPlayerInventory, MachineAdvancedAssemblerBlockEntity pBlockEntity, ContainerData pData) {
        super(ModMenuTypes.MACHINE_PRECASS_MENU.get(), pContainerId, pPlayerInventory, pBlockEntity, pData);
    }

    private static MachineAdvancedAssemblerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        var pos = buffer.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof MachinePrecAssBlockEntity precAss) return precAss;
        throw new IllegalStateException("No MachinePrecAssBlockEntity found at " + pos + " for menu");
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.MACHINE_PRECASS.get());
    }
}
