package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukePrototypeMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class NukePrototypeBlockEntity extends NukeBaseBlockEntity {

    public NukePrototypeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_PROTOTYPE_BE.get(), pos, state, 14);
    }

    // Original GUI is unrestricted: TileEntityNukePrototype.isItemValidForSlot returns false
    // (hoppers only) while 1.7.10 Slot.isItemValid defaults to true, so any item can be placed
    // by the player; readiness is enforced by isReady(). Hoppers have no access at all
    // (NukeBaseBlockEntity.getSlotsForFace returns an empty array, mirroring
    // getAccessibleSlotsFromSide = new int[0]).
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    // Original TileEntityNukePrototype.isReady (TileEntityNukePrototype.java:185-207): SAS-3 cells
    // in slots 0/1/12/13 and a fixed quad-rod pattern U U L L Np Np L L U U in slots 2..11
    // (original compares rod_quad damage = BreedingRodType ordinal; port has one item per type).
    @Override
    public boolean isReady() {
        var sas = ModItems.CELL_SAS3.get();
        var uranium = ModItems.ROD_QUAD_URANIUM.get();
        var lead = ModItems.ROD_QUAD_LEAD.get();
        var np = ModItems.ROD_QUAD_NP237.get();
        net.minecraft.world.item.Item[] pattern = {
            sas, sas, uranium, uranium, lead, lead, np, np, lead, lead, uranium, uranium, sas, sas
        };
        for (int i = 0; i < pattern.length; i++) {
            if (i >= slots.size() || slots.get(i).getItem() != pattern[i]) return false;
        }
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_prototype");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukePrototypeMenu(id, inventory, this);
    }
}
