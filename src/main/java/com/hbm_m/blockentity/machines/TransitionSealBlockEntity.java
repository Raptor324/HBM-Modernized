package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.ILockableTile;
import com.hbm_m.api.tile.LockState;
import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.ILockable;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Core block entity of the transition seal multiblock. A 26 wide x 24 tall x 1 deep
 * blast door driven by the 24 second transition_seal.dae open clip: while redstone
 * power is applied (on the core or anywhere along the frame ring) the clip plays
 * forward and the door opens, otherwise it plays in reverse and the door closes. The
 * clip runs exactly 24 seconds, i.e. 480 ticks, with the frame ring spaced around the
 * core.
 */
public class TransitionSealBlockEntity extends BaseHbmBlockEntity implements ILockableTile, ILockable {

    /** Original TileEntityDoorGeneric extends TileEntityLockableBase: Stiftschloss der Tuer. */
    public final LockState lockState = new LockState();

    /** The transition_seal.dae clip is exactly 24 seconds long (577 LINEAR keyframes at 1/24 s). */
    public static final float DURATION_TICKS = 24F * 20F;

    /** Vertical half span of the frame ring around the core, in blocks. */
    private static final int RING_HEIGHT = 23;
    /** Horizontal half span of the frame ring around the core, in blocks. */
    private static final int RING_WIDTH = 13;


    /** Synchronizes the PASSABLE flag of every phantom part with the door's open state. */
    private void updatePartStates() {
        BlockState state = getBlockState();
        if(!(state.getBlock() instanceof TransitionSealBlock block)) return;
        Direction facing = state.getValue(TransitionSealBlock.FACING);
        boolean open = isOpen();
        for(BlockPos partPos : block.getStructureHelper().getAllPartPositions(worldPosition, facing)) {
            BlockState partState = level.getBlockState(partPos);
            if(partState.hasProperty(UniversalMachinePartBlock.PASSABLE)
                    && partState.getValue(UniversalMachinePartBlock.PASSABLE) != open) {
                level.setBlock(partPos, partState.setValue(UniversalMachinePartBlock.PASSABLE, open), 2);
            }
        }
    }

    /**
     * Returns the animation time in seconds for the given partial tick, clamped to the
     * 24 second clip. The open clip is played forward while powered and reversed while
     * unpowered; the finished pose is held once the door is fully open or closed.
     */
    public float getAnimationTime(float partialTicks) {
        float time = animTicks;
        if(moving) {
            time += opening ? partialTicks : -partialTicks;
        }
        return Math.max(0F, Math.min(DURATION_TICKS, time)) / 20F;
    }

    public boolean isOpen() {
        return animTicks >= DURATION_TICKS;
    }

    /** Normalized opening progress in [0, 1] for progressive collision. */
    public float getOpenProgress() {
        return Math.max(0F, Math.min(1F, animTicks / DURATION_TICKS));
    }

    /**
     * True while the core or any block of the 26 x 24 multiblock wall is receiving
     * redstone power (1.7.10 BlockDoorGeneric.onNeighborBlockChange parity).
     */
    public boolean isPowered() {
        if(level == null) return false;
        if(level.hasNeighborSignal(worldPosition)) return true;

        boolean zAxis = isZAxis();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        int cx = worldPosition.getX();
        int cy = worldPosition.getY();
        int cz = worldPosition.getZ();
        for(int dy = 0; dy <= RING_HEIGHT; dy++) {
            for(int dw = -RING_WIDTH; dw <= RING_WIDTH; dw++) {
                if(zAxis) {
                    checkPos.set(cx, cy + dy, cz + dw);
                } else {
                    checkPos.set(cx + dw, cy + dy, cz);
                }
                if(level.hasNeighborSignal(checkPos)) return true;
            }
        }
        return false;
    }

    /** True while the 26 block wall width runs along the z axis (east/west facing). */
    private boolean isZAxis() {
        if(level == null) return false;
        Direction facing = level.getBlockState(worldPosition).getValue(TransitionSealBlock.FACING);
        return facing == Direction.EAST || facing == Direction.WEST;
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        // The seal is 26 wide (radius 13), 24 tall at rest, but reaches Y=42 when fully open.
        return new AABB(
                worldPosition.getX() - 13, worldPosition.getY(), worldPosition.getZ() - 13,
                worldPosition.getX() + 14, worldPosition.getY() + 44, worldPosition.getZ() + 14
        );
    }

    // Animation state
    public float animTicks = 0F;
    public boolean moving = false;
    /** Target direction while animating: true = opening, false = closing */
    public boolean opening = false;
    /** Previous redstone state used to implement toggle-on-pulse behaviour */
    public boolean prevPowered = false;

    public TransitionSealBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TRANSITION_SEAL_BE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TransitionSealBlockEntity be) {
        be.updateEntity();
    }

    /**
     * Cached result of the ~75-position {@link #isPowered()} frame-ring sweep. The sweep is
     * expensive (hasNeighborSignal per ring block), so it runs only on {@code probeDue(40)}
     * (plus the first server tick); between sweeps the cached value is reused. PR-note: a
     * redstone edge anywhere on the frame can take up to 40 ticks to be noticed - no
     * neighbor-changed hook was verified, this latency is the accepted fidelity cost.
     */
    private boolean cachedPowered = false;
    private boolean cachedPoweredValid = false;

    /** Server-side cached power read; client still sweeps every tick (render parity). */
    private boolean cachedPowered() {
        if(!cachedPoweredValid || probeDue(40)) {
            cachedPowered = isPowered();
            cachedPoweredValid = true;
        }
        return cachedPowered;
    }

    public void invalidateRedstoneCache() {
        this.cachedPoweredValid = false;
    }

    // ==================== Schloss (Original TileEntityLockableBase / TileEntityDoorGeneric) ====================

    @Override public LockState getLockState() { return lockState; }
    @Override public boolean isLocked() { return lockState.isLocked; }

    @Override
    public void lock() {
        lockState.lock(this);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void unlock() {
        lockState.isLocked = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public void setPins(int pins) { lockState.lock = pins; setChanged(); }
    @Override public int getPins() { return lockState.lock; }
    @Override public void setMod(double mod) { lockState.lockMod = mod; setChanged(); }
    @Override public double getMod() { return lockState.lockMod; }
    @Override public boolean isCheesable() { return lockState.cheesable; }
    @Override public void setCheesable(boolean cheesable) { lockState.cheesable = cheesable; setChanged(); }

    @Override public double getLockMod() { return lockState.lockMod; }
    @Override public void setLockMod(double mod) { lockState.lockMod = mod; setChanged(); }
    @Override public BlockPos getLockPos() { return worldPosition; }
    @Override public Level getLockLevel() { return level; }

    /** Display notification above hotbar when access is denied. */
    public void notifyLocked(@Nullable net.minecraft.world.entity.player.Player player) {
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.hbm_m.door_locked"), true);
        }
    }

    /**
     * audit10: Original {@code TileEntityDoorGeneric.tryToggle(player)} - nur ganz offen oder ganz zu schaltbar;
     * ein geschlossenes Tor unter Redstone-Strom bleibt "verriegelt".
     */
    public boolean tryToggle() {
        return tryToggle(null);
    }

    public boolean tryToggle(@Nullable net.minecraft.world.entity.player.Player player) {
        if (isLocked() && player == null) return false;
        if (moving) return false;
        if (!isOpen() && isPowered()) return false;

        if (level != null && !level.isClientSide) {
            if (canAccess(player)) {
                opening = !isOpen();
                moving = true;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            } else {
                notifyLocked(player);
            }
        }
        return true;
    }

    public void updateEntity() {
        if(level == null) return;

        boolean powered = cachedPowered();
        float prev = animTicks;
        boolean prevOpen = prev >= DURATION_TICKS;
        boolean wasMoving = moving;

        // 1.7.10 TileEntityDoorGeneric parity: level-sensitive redstone control.
        // Signal ON opens a closed door; falling edge (signal OFF) closes an open door.
        // A short pulse (e.g. button) turns off while still opening, so the door stays open.
        if(!moving && !isLocked()) {
            if(powered && !isOpen()) {
                opening = true;
                moving = true;
                if(!level.isClientSide) {
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            } else if(!powered && prevPowered && isOpen()) {
                opening = false;
                moving = true;
                if(!level.isClientSide) {
                    setChanged();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        }
        prevPowered = powered;

        if(moving) {
            if(opening && animTicks < DURATION_TICKS) {
                animTicks = Math.min(DURATION_TICKS, animTicks + 1F);
            }
            else if(!opening && animTicks > 0F) {
                animTicks = Math.max(0F, animTicks - 1F);
            }
        }

        moving = animTicks > 0F && animTicks < DURATION_TICKS;

        if(!level.isClientSide) {
            if(moving && !wasMoving) {
                // 1.7.10 parity: DoorDecl.getCloseSoundStart() defaults to the open
                // sound - the same long mechanical track plays in both directions. Sound volume 6.0F.
                // DoorDecl.TRANSITION_SEAL has no open/close end sound (null).
                level.playSound(null, worldPosition, ModSounds.TRANSITION_SEAL_OPEN.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
            }
            if(animTicks != prev) setChanged();
            if(isOpen() != prevOpen) {
                updatePartStates();
                setChanged();
            }
        }
    }

    
    // (RenderBoundsProvider уже реализован в BaseHbmBlockEntity; собственный
    // getRenderBoundingBox сохранён — AABB двери 26x24 больше дефолтного)
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putFloat("animTicks", animTicks);
        tag.putBoolean("prevPowered", prevPowered);
        tag.putBoolean("opening", opening);
        tag.putBoolean("moving", moving);
        lockState.write(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        animTicks = tag.getFloat("animTicks");
        prevPowered = tag.getBoolean("prevPowered");
        opening = tag.getBoolean("opening");
        moving = tag.getBoolean("moving");
        if (tag.contains("lockMod")) {
            lockState.read(tag);
        } else {
            lockState.isLocked = tag.getBoolean("locked");
            lockState.lock = tag.getInt("lock");
        }
    }
}
