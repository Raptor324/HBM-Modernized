package com.hbm_m.blockentity.decorations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.api.tile.LockState;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.FileCabinetMenu;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityFileCabinet}: 8 Faecher, abschliessbar ({@link LockState}), keine Automatisierung. Wer die
 * Schublade offen hat, zaehlt als Benutzer; die untere Schublade faehrt sofort, die obere nach 10 Ticks heraus
 * (Klang wie im Original), Zustand geht jeden Tick an Spieler im Umkreis 25.
 */
public class FileCabinetBlockEntity extends BaseHbmBlockEntity implements MenuProvider, ILockableTile {

    public final ModItemStackHandler items = new ModItemStackHandler(8) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    public final LockState lockState = new LockState();

    private int timer = 0;
    private int playersUsing = 0;
    public float lowerExtent = 0;
    public float prevLowerExtent = 0;
    public float upperExtent = 0;
    public float prevUpperExtent = 0;

    public FileCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FILE_CABINET.get(), pos, state);
    }

    public void startOpen() { if (level != null && !level.isClientSide) playersUsing++; }
    public void stopOpen() { if (level != null && !level.isClientSide) playersUsing--; }

    public static void tick(Level world, BlockPos pos, BlockState state, FileCabinetBlockEntity te) {
        if (!world.isClientSide) {
            if (te.playersUsing > 0) {
                if (te.timer < 10) te.timer++;
            } else {
                te.timer = 0;
            }
            te.networkPackNT((ServerLevel) world, 25);
        } else {
            te.prevLowerExtent = te.lowerExtent;
            te.prevUpperExtent = te.upperExtent;
        }

        float openSpeed = te.playersUsing > 0 ? 1F / 16F : 1F / 25F;
        float maxExtent = 0.8F;

        if (te.playersUsing > 0) {
            if (te.lowerExtent == 0F && te.upperExtent == 0F) {
                te.sound(world, "block.crateOpen", 0.8F, 1.0F);
            } else {
                if (te.upperExtent + openSpeed >= maxExtent && te.lowerExtent < maxExtent)
                    te.sound(world, "block.crateOpen", 0.5F, world.random.nextFloat() * 0.1F + 0.7F);
                if (te.lowerExtent + openSpeed >= maxExtent && te.lowerExtent < maxExtent)
                    te.sound(world, "block.crateOpen", 0.5F, world.random.nextFloat() * 0.1F + 0.7F);
            }
            te.lowerExtent += openSpeed;
            if (te.timer >= 10) te.upperExtent += openSpeed;
        } else if (te.lowerExtent > 0) {
            if (te.upperExtent - openSpeed < maxExtent / 2 && te.upperExtent >= maxExtent / 2 && te.upperExtent != te.lowerExtent)
                te.sound(world, "block.crateClose", 0.8F, 1.0F);
            if (te.lowerExtent - openSpeed < maxExtent / 2 && te.lowerExtent >= maxExtent / 2)
                te.sound(world, "block.crateClose", 0.8F, 1.0F);
            te.upperExtent -= openSpeed;
            te.lowerExtent -= openSpeed;
        }

        te.lowerExtent = Mth.clamp(te.lowerExtent, 0F, maxExtent);
        te.upperExtent = Mth.clamp(te.upperExtent, 0F, maxExtent);
    }

    /** {@code worldObj.playSoundEffect}: nur der Server spielt ab. */
    private void sound(Level world, String key, float volume, float pitch) {
        if (!world.isClientSide) {
            world.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, HbmSoundsNT.get(key), SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void networkPackNT(ServerLevel server, int range) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("timer", timer);
        tag.putInt("playersUsing", playersUsing);
        lockState.write(tag);
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this, be -> tag);
        for (var player : server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= (double) range * range)
                player.connection.send(packet);
        }
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        if (tag.contains("timer")) {
            timer = tag.getInt("timer");
            playersUsing = tag.getInt("playersUsing");
            lockState.read(tag);
        } else {
            readNbtData(tag, null);
        }
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.put("items", items.serializeNBT());
        lockState.write(nbt);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        if (nbt.contains("items")) items.deserializeNBT(nbt.getCompound("items"));
        lockState.read(nbt);
    }

    public boolean canAccess(Player player) {
        return lockState.canAccess(level, player);
    }

    // ---- ILockableTile ----
    @Override public boolean isLocked() { return lockState.isLocked; }
    @Override public void lock() { lockState.isLocked = true; setChanged(); }
    @Override public void unlock() { lockState.isLocked = false; setChanged(); }
    @Override public void setPins(int pins) { lockState.lock = pins; setChanged(); }
    @Override public int getPins() { return lockState.lock; }
    @Override public void setMod(double mod) { lockState.lockMod = mod; setChanged(); }
    @Override public double getMod() { return lockState.lockMod; }
    @Override public boolean isCheesable() { return lockState.cheesable; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.file_cabinet");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new FileCabinetMenu(containerId, inv, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
    }
}
