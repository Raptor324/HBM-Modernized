package com.hbm_m.blockentity.machines;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachinePrecAssMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMachinePrecAss} (1.7.10 Original) - "Precision Assembler". Per the
 * original author's own comment ("horribly copy-pasted crap device"), this is mechanically a
 * near-clone of the Advanced Assembler (same blueprint-folder-driven recipe selection, same
 * 4000mB fluid I/O tanks) with only a different multiblock shell and animated striker/ring visuals
 * - this port reuses {@link MachineAdvancedAssemblerBlockEntity} wholesale, same pattern as
 * Bat9000/Orbus in this port.
 * <p>
 * SCOPE-Vereinfachung: Die animierten Arme/Schlagstoessel/rotierenden Ringe (rein optisch) und der
 * Sound-Loop entfallen; die Blaupausen-Rezeptauswahl, Fluid-I/O und Energie-/Upgrade-Slots bleiben
 * vollstaendig erhalten.
 */
public class MachinePrecAssBlockEntity extends MachineAdvancedAssemblerBlockEntity {

    public MachinePrecAssBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PRECASS_BE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.MACHINE_PRECASS.get().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachinePrecAssMenu(id, inventory, this, this.data);
    }
}
