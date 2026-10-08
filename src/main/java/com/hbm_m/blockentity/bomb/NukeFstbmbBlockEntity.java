package com.hbm_m.blockentity.bomb;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.logic.EntityBalefireExplosion;
import com.hbm_m.inventory.menu.NukeFstbmbMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityNukeBalefire}: Slot 0 Balefire-Ei, Slot 1 Funken-/Trixit-Batterie. Ist die Bombe
 * scharf ({@code started}), zaehlt der Timer herunter (Standard 18000 Ticks), piept jede Sekunde und zuendet bei 0.
 * Faellt ein Teil heraus, bricht der Countdown ab. GUI-Knoepfe (Original {@code AuxButtonPacket}) kommen
 * ueber {@link IControlReceiver}: {@code meta 0} = Start, {@code meta 1} = Timer in Sekunden.
 */
public class NukeFstbmbBlockEntity extends NukeBaseBlockEntity implements IControlReceiver {

    public static final int SLOTS = 2;

    public boolean loaded;
    public boolean started;
    public int timer = 18000;

    public NukeFstbmbBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_FSTBMB_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_fstbmb");
    }

    /** Serverseitiger Tick (Original {@code updateEntity}). */
    public void serverTick() {
        if (level == null || level.isClientSide) return;

        boolean wasLoaded = this.loaded;
        this.loaded = this.isLoaded();

        if (!loaded) {
            started = false;
        }

        if (started) {
            timer--;

            if (timer % 20 == 0)
                level.playSound(null, worldPosition, HbmSoundsNT.get("hbm:weapon.fstbmbPing"), SoundSource.BLOCKS, 5.0F, 1.0F);
        }

        if (timer <= 0) {
            explode();
            return;
        }

        // Original networkPackNT(250): Timer/Status laufend an die Clients
        // (auch beim Wechsel von loaded: der Renderer zeigt Glanz und Zeitanzeige nur geladen)
        if (started || loaded != wasLoaded) sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original {@code handleButtonPacket(value, meta)}. */
    public void handleButtonPacket(int value, int meta) {
        if (level == null) return;

        if (meta == 0 && this.isLoaded()) {
            level.playSound(null, worldPosition, HbmSoundsNT.get("hbm:weapon.fstbmbStart"), SoundSource.BLOCKS, 5.0F, 1.0F);
            started = true;
        }

        if (meta == 1)
            timer = value * 20;

        sync();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        handleButtonPacket(data.getInt("value"), data.getInt("meta"));
    }

    public boolean isLoaded() {
        return hasEgg() && hasBattery();
    }

    public boolean hasEgg() {
        return slots.get(0).is(ModItems.EGG_BALEFIRE.get());
    }

    public boolean hasBattery() {
        return getBattery() > 0;
    }

    public int getBattery() {
        if (slots.get(1).is(ModItems.BATTERY_SPARK.get())) return 1;
        if (slots.get(1).is(ModItems.BATTERY_TRIXITE.get())) return 2;
        return 0;
    }

    /** Original {@code explode}: Inhalt weg, Block ohne Drop zerstoeren, Balefire-Explosion mit Radius 250. */
    public void explode() {
        if (level == null) return;

        clearContent();

        level.destroyBlock(worldPosition, false);

        EntityBalefireExplosion bf = EntityBalefireExplosion.statFac(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 250);
        level.addFreshEntity(bf);
        // Original EntityNukeTorex.statFacBale: der Pilz wird von EntityBalefireExplosion selbst erzeugt
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        if (mins.length() == 1) mins = "0" + mins;
        return mins;
    }

    public String getSeconds() {
        String mins = "" + ((timer / 20) % 60);
        if (mins.length() == 1) mins = "0" + mins;
        return mins;
    }

    /** Original: TileEntityMachineBase-Standard (Stapelgrenze 64, im GUI jeder Gegenstand). */
    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction direction) {
        return new int[0];
    }

    @Override
    public boolean isReady() {
        return isLoaded();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new NukeFstbmbMenu(id, inventory, this);
    }

    @Override
    protected void readNbtData(@org.jetbrains.annotations.NotNull CompoundTag tag,
                               @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.started = tag.getBoolean("started");
        this.timer = tag.getInt("timer");
        this.loaded = tag.getBoolean("loaded");
    }

    @Override
    protected void writeNbtData(@org.jetbrains.annotations.NotNull CompoundTag tag,
                                @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("started", started);
        tag.putInt("timer", timer);
        tag.putBoolean("loaded", loaded);
    }
}
