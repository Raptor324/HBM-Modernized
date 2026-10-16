package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.ModSounds;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMachineSiren} (1.7.10 Original) - redstone-triggered, holds a single
 * cassette item selecting the alarm track to play.
 * <p>
 * SCOPE-Vereinfachung: Das Original haelt einen echten Client-seitigen Loop-Soundinstance offen
 * (per {@code TESirenPacket} gestartet/gestoppt) und unterstuetzt LOOP/PASS/SOUND-Wiedergabearten
 * ueber 20 Tracks. Hier: nur die 7 Tracks mit portiertem .ogg (alle original LOOP-Typ) - simuliert
 * per periodischem Retrigger von {@link Level#playSound} statt einer echten nahtlosen Loop-Instanz.
 */
public class MachineSirenBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 1;
    private static final int RETRIGGER_PERIOD = 100;

    private boolean wasPowered = false;
    private int retriggerTimer = 0;

    public MachineSirenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SIREN_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSirenBlockEntity be) {
        if (level.isClientSide) return;

        boolean powered = level.hasNeighborSignal(pos);
        RegistrySupplier<SoundEvent> sound = getSound(be.inventory.getStackInSlot(0).getItem());

        if (sound == null || !powered) {
            be.wasPowered = false;
            be.retriggerTimer = 0;
            return;
        }

        if (!be.wasPowered || be.retriggerTimer <= 0) {
            level.playSound(null, pos, sound.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
            be.retriggerTimer = RETRIGGER_PERIOD;
        } else {
            be.retriggerTimer--;
        }
        be.wasPowered = true;
    }

    private static RegistrySupplier<SoundEvent> getSound(Item item) {
        if (item == ModItems.CASSETTE_AMS_SIREN.get()) return ModSounds.SIREN_AMS;
        if (item == ModItems.CASSETTE_BEEP_SIREN.get()) return ModSounds.SIREN_BEEP;
        if (item == ModItems.CASSETTE_CLASSIC_SIREN.get()) return ModSounds.SIREN_CLASSIC;
        if (item == ModItems.CASSETTE_NOSTROMO_SIREN.get()) return ModSounds.SIREN_NOSTROMO;
        if (item == ModItems.CASSETTE_REGULAR_SIREN.get()) return ModSounds.SIREN_REGULAR;
        if (item == ModItems.CASSETTE_STRIDER_SIREN.get()) return ModSounds.SIREN_STRIDER;
        if (item == ModItems.CASSETTE_SWEEP_SIREN.get()) return ModSounds.SIREN_SWEEP;
        return null;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return getSound(stack.getItem()) != null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_siren");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineSirenMenu.create(id, inventory, this);
    }
}
