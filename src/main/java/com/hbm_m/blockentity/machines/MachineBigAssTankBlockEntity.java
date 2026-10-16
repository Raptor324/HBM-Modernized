package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.FluidTankMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code TileEntityMachineBigAssTank} (1.7.10): der grosse Lagertank.
 *
 * <p>Im Original schlicht ein {@code TileEntityBarrel} mit 16 Millionen mB.</p>
 *
 * <p><b>Abweichungen:</b></p>
 * <ul>
 *   <li>Die Durchsatzformeln des Originals ({@code max(50_000, freiesVolumen / 100)}) sind
 *   <b>nicht</b> uebernommen. {@link MachineFluidTankBlockEntity} skaliert den Durchsatz bereits
 *   nach Fuellstand und deckelt ihn zusaetzlich auf die Haelfte des Inhalts je Tick - ohne diesen
 *   Deckel koennten zwei gleich grosse Tanks ihren Inhalt jeden Tick vollstaendig tauschen. Die
 *   Originalformel kennt diesen Schutz nicht; sie hier einzusetzen waere ein Rueckschritt.</li>
 *   <li>Die Kippkontrolle des Originals ({@code checkTilt}) ist projektweit nicht portiert.</li>
 * </ul>
 *
 * <p>Antimaterie sprengt den Tank wie im Original - das erledigt bereits die geerbte Pruefung aus
 * {@link MachineFluidTankBlockEntity}, ohne eigene Ueberschreibung.</p>
 */
public class MachineBigAssTankBlockEntity extends MachineFluidTankBlockEntity {

    /** Original: {@code super(16_000_000)}. */
    public static final int CAPACITY = 16_000_000;

    public MachineBigAssTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIGASSTANK_BE.get(), pos, state, CAPACITY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.MACHINE_BIGASSTANK.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FluidTankMenu(id, inventory, this, this.data);
    }
}
