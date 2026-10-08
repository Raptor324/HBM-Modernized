package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.api.tile.LockState;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityBlastDoor}: 7 Bloecke hohes Tor; Zustand 0 zu, 1 in Bewegung, 2 offen. Oeffnen raeumt die
 * Rahmenbloecke y+1..5 alle 20 Ticks von unten, Schliessen setzt sie von oben wieder. Nachbar-Tore (gleiches Schloss
 * oder unverschlossen) schalten mit, Redstone am Fuss oder Kopf schaltet um.
 */
public class BlastDoorBlockEntity extends BaseHbmBlockEntity implements ILockableTile {

    public final LockState lockState = new LockState();
    public boolean isOpening = false;
    /** 0: zu, 1: oeffnet/schliesst, 2: offen */
    public int state = 0;
    /** Nur Client: Startzeit der Animation. */
    public long sysTime;
    private int timer = 0;
    public boolean redstoned = false;

    public BlastDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_DOOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState bs, BlastDoorBlockEntity te) {
        // Klammerung 1:1: (!locked && Fuss bestromt) || Kopf bestromt
        if (!te.isLocked() && level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above(6))) {
            if (!te.redstoned) te.tryToggle();
            te.redstoned = true;
        } else {
            te.redstoned = false;
        }

        if (te.state != 1) {
            te.timer = 0;
        } else {
            te.timer++;
            if (te.isOpening) {
                if (te.timer >= 0) te.removeDummy(pos.above(1));
                if (te.timer >= 20) te.removeDummy(pos.above(2));
                if (te.timer >= 40) te.removeDummy(pos.above(3));
                if (te.timer >= 60) te.removeDummy(pos.above(4));
                if (te.timer >= 80) te.removeDummy(pos.above(5));
            } else {
                if (te.timer >= 20) te.placeDummy(pos.above(5));
                if (te.timer >= 40) te.placeDummy(pos.above(4));
                if (te.timer >= 60) te.placeDummy(pos.above(3));
                if (te.timer >= 80) te.placeDummy(pos.above(2));
                if (te.timer >= 100) te.placeDummy(pos.above(1));
            }
            if (te.timer >= 100) {
                if (te.isOpening) te.finishOpen();
                else te.finishClose();
            }
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void sound(String key, float vol, float pitch) {
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), HbmSoundsNT.get(key), SoundSource.BLOCKS, vol, pitch);
    }

    public void open() {
        if (state == 0) {
            isOpening = true;
            state = 1;
            sound("block.reactorStart", 0.5F, 0.75F);
            sync();
        }
    }

    public void finishOpen() {
        state = 2;
        sound("block.reactorStop", 0.5F, 1.0F);
        sync();
    }

    public void close() {
        if (state == 2) {
            isOpening = false;
            state = 1;
            sound("block.reactorStart", 0.5F, 0.75F);
            sync();
        }
    }

    public void finishClose() {
        state = 0;
        sound("block.reactorStop", 0.5F, 1.0F);
        sync();
    }

    private BlastDoorBlockEntity[] neighbours() {
        BlastDoorBlockEntity[] out = new BlastDoorBlockEntity[4];
        BlockPos[] ps = { worldPosition.east(), worldPosition.west(), worldPosition.south(), worldPosition.north() };
        for (int i = 0; i < 4; i++) if (level.getBlockEntity(ps[i]) instanceof BlastDoorBlockEntity d) out[i] = d;
        return out;
    }

    public void openNeigh() {
        for (BlastDoorBlockEntity d : neighbours()) {
            if (d != null && d.canOpen() && (!d.isLocked() || d.lockState.lock == lockState.lock)) {
                d.open();
                d.openNeigh();
            }
        }
    }

    public void closeNeigh() {
        for (BlastDoorBlockEntity d : neighbours()) {
            if (d != null && d.canClose() && (!d.isLocked() || d.lockState.lock == lockState.lock)) {
                d.close();
                d.closeNeigh();
            }
        }
    }

    public void lockNeigh() {
        for (BlastDoorBlockEntity d : neighbours()) {
            if (d != null && !d.isLocked()) {
                d.setPins(lockState.lock);
                d.lock();
                d.setMod(lockState.lockMod);
            }
        }
    }

    public boolean canOpen() { return state == 0; }
    public boolean canClose() { return state == 2; }

    public void tryToggle() {
        if (canOpen()) {
            open();
            openNeigh();
        } else if (canClose()) {
            close();
            closeNeigh();
        }
    }

    public boolean placeDummy(BlockPos p) {
        BlockState present = level.getBlockState(p);
        if (!present.canBeReplaced() && !present.is(ModBlocks.DUMMY_BLOCK_BLAST.get())) level.destroyBlock(p, false);
        level.setBlockAndUpdate(p, ModBlocks.DUMMY_BLOCK_BLAST.get().defaultBlockState());
        if (level.getBlockEntity(p) instanceof com.hbm_m.block.machines.BlastDoorBlock.DummyBlockEntity dummy) dummy.setTarget(worldPosition);
        return true;
    }

    public void removeDummy(BlockPos p) {
        if (level.getBlockState(p).is(ModBlocks.DUMMY_BLOCK_BLAST.get())) {
            com.hbm_m.block.machines.BlastDoorBlock.Dummy.safeBreak = true;
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            com.hbm_m.block.machines.BlastDoorBlock.Dummy.safeBreak = false;
        }
    }

    public boolean canAccess(Player player) { return lockState.canAccess(level, player); }

    // ---- ILockableTile ----
    @Override public com.hbm_m.api.tile.LockState getLockState() { return lockState; }
    @Override public boolean isLocked() { return lockState.isLocked; }
    @Override public void lock() { lockState.lock(this); setChanged(); lockNeigh(); }
    @Override public void unlock() { lockState.isLocked = false; setChanged(); }
    @Override public void setPins(int pins) { lockState.lock = pins; setChanged(); }
    @Override public int getPins() { return lockState.lock; }
    @Override public void setMod(double mod) { lockState.lockMod = mod; setChanged(); }
    @Override public double getMod() { return lockState.lockMod; }
    @Override public boolean isCheesable() { return lockState.cheesable; }

    @Override
    public AABB getRenderBoundingBox() { return INFINITE_EXTENT_AABB; }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        int prevState = state;
        readNbtData(tag, null);
        // TEVaultPacket Typ 1: Animationsbeginn beim Wechsel in den Bewegungszustand
        if (state == 1 && prevState != 1) sysTime = System.currentTimeMillis();
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        lockState.write(nbt);
        nbt.putBoolean("isOpening", isOpening);
        nbt.putInt("state", state);
        nbt.putInt("timer", timer);
        nbt.putBoolean("redstoned", redstoned);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        lockState.read(nbt);
        isOpening = nbt.getBoolean("isOpening");
        state = nbt.getInt("state");
        timer = nbt.getInt("timer");
        redstoned = nbt.getBoolean("redstoned");
    }
}
