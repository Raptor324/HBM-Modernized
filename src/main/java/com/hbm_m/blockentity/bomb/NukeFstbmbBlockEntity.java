package com.hbm_m.blockentity.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.NukeFstbmbMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE бомбы бейлфайра: 2 слота (яйцо бейлфайра + батарея).
 * Тикает таймер после активации, по истечении — взрыв.
 */
public class NukeFstbmbBlockEntity extends NukeBaseBlockEntity {

    public static final int SLOTS = 2;

    public int timer = 18000;
    public boolean started = false;

    public NukeFstbmbBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_FSTBMB_BE.get(), pos, state, SLOTS);
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.hbm_m.nuke_fstbmb");
    }

    public boolean isLoaded() {
        return slots.get(0).is(ModItems.EGG_BALEFIRE.get())
                && (slots.get(1).is(ModItems.BATTERY_SPARK.get()) || slots.get(1).is(ModItems.BATTERY_TRIXITE.get()));
    }

    /** Вызывается из GUI: запуск отсчёта. */
    public void startCountdown() {
        if (!isReady() || started) return;
        started = true;
        if (level != null && !level.isClientSide) {
            PlatformHooks.playSound(level, worldPosition,
                    net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 3.0F, 1.0F);
            setChanged();
        }
    }

    public void setTimerMinutes(int minutes) {
        this.timer = minutes * 60 * 20;
        setChanged();
    }

    /** Серверный тик блока: отсчёт и подрыв. */
    public void serverTick() {
        if (level == null || level.isClientSide || !started) return;
        if (!isLoaded()) return;
        timer--;
        if (timer % 20 == 0) {
            PlatformHooks.playSound(level, worldPosition,
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.5F);
        }
        if (timer <= 0) {
            started = false;
            if (getBlockState().getBlock() instanceof com.hbm_m.block.bomb.NukeFstbmbBlock bomb) {
                bomb.explode(level, worldPosition);
            }
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 ? stack.is(ModItems.EGG_BALEFIRE.get())
                : stack.is(ModItems.BATTERY_SPARK.get()) || stack.is(ModItems.BATTERY_TRIXITE.get());
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
    protected void readNbtData(@org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag,
                               @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.timer = tag.getInt("timer");
        this.started = tag.getBoolean("started");
    }

    @Override
    protected void writeNbtData(@org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag,
                                @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("timer", timer);
        tag.putBoolean("started", started);
    }
}
