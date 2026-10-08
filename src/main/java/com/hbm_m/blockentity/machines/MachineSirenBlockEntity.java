package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityMachineSiren}: Kassette im Platz 0 waehlt den Titel ({@code ItemCassette.TrackType}).
 * LOOP-Titel laufen als Schleife, solange Redstone anliegt; PASS/SOUND spielen bei jeder steigenden Flanke einmal.
 * Den Klang spielt der Client ({@code TESirenPacket}/{@code SoundLoopSiren}) - im Port ueber den Blockentity-Sync und
 * {@code client.sound.SirenSoundClient}, Reichweite = {@code TrackType.getVolume()}.
 */
public class MachineSirenBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 1;

    public enum SoundType { LOOP, PASS, SOUND }

    /** Original {@code ItemCassette.TrackType} (Titel, Klang, Art, Farbe, Reichweite). */
    public enum TrackType {
        NULL(" ", null, SoundType.SOUND, 0, 0),
        HATCH("Hatch Siren", "hbm:alarm.hatch", SoundType.LOOP, 3358839, 250),
        ATUOPILOT("Autopilot Disconnected", "hbm:alarm.autopilot", SoundType.LOOP, 11908533, 50),
        AMS_SIREN("AMS Siren", "hbm:alarm.amsSiren", SoundType.LOOP, 15055698, 50),
        BLAST_DOOR("Blast Door Alarm", "hbm:alarm.blastDoorAlarm", SoundType.LOOP, 11665408, 50),
        APC_LOOP("APC Siren", "hbm:alarm.apcLoop", SoundType.LOOP, 3565216, 50),
        KLAXON("Klaxon", "hbm:alarm.klaxon", SoundType.LOOP, 8421504, 50),
        KLAXON_A("Vault Door Alarm", "hbm:alarm.foKlaxonA", SoundType.LOOP, 0x8c810b, 50),
        KLAXON_B("Security Alert", "hbm:alarm.foKlaxonB", SoundType.LOOP, 0x76818e, 50),
        SIREN("Standard Siren", "hbm:alarm.regularSiren", SoundType.LOOP, 6684672, 100),
        CLASSIC("Classic Siren", "hbm:alarm.classic", SoundType.LOOP, 0xc0cfe8, 100),
        BANK_ALARM("Bank Alarm", "hbm:alarm.bankAlarm", SoundType.LOOP, 3572962, 100),
        BEEP_SIREN("Beep Siren", "hbm:alarm.beepSiren", SoundType.LOOP, 13882323, 100),
        CONTAINER_ALARM("Container Alarm", "hbm:alarm.containerAlarm", SoundType.LOOP, 14727839, 100),
        SWEEP_SIREN("Sweep Siren", "hbm:alarm.sweepSiren", SoundType.LOOP, 15592026, 500),
        STRIDER_SIREN("Missile Silo Siren", "hbm:alarm.striderSiren", SoundType.LOOP, 11250586, 500),
        AIR_RAID("Air Raid Siren", "hbm:alarm.airRaid", SoundType.LOOP, 0xDF3795, 500),
        NOSTROMO_SIREN("Nostromo Self Destruct", "hbm:alarm.nostromoSiren", SoundType.LOOP, 0x5dd800, 100),
        EAS_ALARM("EAS Alarm Screech", "hbm:alarm.easAlarm", SoundType.LOOP, 0xb3a8c1, 50),
        APC_PASS("APC Pass", "hbm:alarm.apcPass", SoundType.PASS, 3422163, 50),
        RAZORTRAIN("Razortrain Horn", "hbm:alarm.razortrainHorn", SoundType.SOUND, 7819501, 250);

        public final String title;
        public final String sound;
        public final SoundType type;
        public final int color;
        public final int volume;

        TrackType(String title, String sound, SoundType type, int color, int volume) {
            this.title = title;
            this.sound = sound;
            this.type = type;
            this.color = color;
            this.volume = volume;
        }

        public static TrackType getEnum(int i) {
            return i >= 0 && i < values().length ? values()[i] : NULL;
        }
    }

    public boolean lock = false;

    /** Client-Sync: aktueller Titel, LOOP an/aus, Zaehler der PASS/SOUND-Ausloesungen. */
    private int syncTrack = 0;
    private boolean syncActive = false;
    private int syncPulse = 0;
    private int clientPulse = 0;

    public MachineSirenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SIREN_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSirenBlockEntity be) {
        if (level.isClientSide) {
            be.clientTick();
            return;
        }

        TrackType type = be.getCurrentType();
        int id = type.ordinal();
        boolean active = false;
        int pulse = be.syncPulse;

        if (type != TrackType.NULL) {
            boolean powered = level.hasNeighborSignal(pos);

            if (type.type == SoundType.LOOP) {
                active = powered;
            } else {
                if (!be.lock && powered) {
                    be.lock = true;
                    pulse++;
                    active = true;
                }
                if (be.lock && !powered) {
                    be.lock = false;
                }
                active = be.lock;
            }
        }

        if (id != be.syncTrack || active != be.syncActive || pulse != be.syncPulse) {
            be.syncTrack = id;
            be.syncActive = active;
            be.syncPulse = pulse;
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    private void clientTick() {
        TrackType type = TrackType.getEnum(syncTrack);
        boolean restart = syncPulse != clientPulse;
        clientPulse = syncPulse;
        boolean loop = type.type == SoundType.LOOP;
        try {
            Class.forName("com.hbm_m.client.sound.SirenSoundClient")
                    .getMethod("update", net.minecraft.world.level.block.entity.BlockEntity.class, String.class, int.class, boolean.class, boolean.class, boolean.class)
                    .invoke(null, this, type.sound, type.volume, loop, loop ? syncActive : restart, restart);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public TrackType getCurrentType() {
        return getTrack(inventory.getStackInSlot(0).getItem());
    }

    /** Original {@code ItemCassette.getType(stack)}: der Track steckt im Kassetten-Gegenstand. */
    public static TrackType getTrack(Item item) {
        return item instanceof com.hbm_m.item.machine.ItemCassette c ? c.track : TrackType.NULL;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return getTrack(stack.getItem()) != TrackType.NULL;
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

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("track", syncTrack);
        tag.putBoolean("active", syncActive);
        tag.putInt("pulse", syncPulse);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        syncTrack = tag.getInt("track");
        syncActive = tag.getBoolean("active");
        syncPulse = tag.getInt("pulse");
        if (level == null) clientPulse = syncPulse;
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slot {0}; weder hinein noch heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Slot {0}; weder hinein noch heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 0 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
