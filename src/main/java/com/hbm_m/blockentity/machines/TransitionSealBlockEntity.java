package com.hbm_m.blockentity.machines;

import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.block.machines.TransitionSealBlock;
import com.hbm_m.blockentity.ModBlockEntities;
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
public class TransitionSealBlockEntity extends BlockEntity {

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

    /**
     * True while the core or any block of the frame ring is receiving redstone power.
     * The frame ring surrounds the 26 x 24 wall, so a button or lever anywhere on the
     * frame operates the door.
     */
    public boolean isPowered() {
        if(level == null) return false;
        if(level.hasNeighborSignal(worldPosition)) return true;

        boolean zAxis = isZAxis();
        for(int dy = 0; dy <= RING_HEIGHT; dy++) {
            for(int dz : new int[] { -RING_WIDTH, RING_WIDTH }) {
                if(hasSignalAt(dy, dz, zAxis)) return true;
            }
        }
        for(int dz = -RING_WIDTH; dz <= RING_WIDTH; dz++) {
            if(hasSignalAt(0, dz, zAxis) || hasSignalAt(RING_HEIGHT, dz, zAxis)) return true;
        }
        return false;
    }

    /** True while the 26 block wall width runs along the z axis (east/west facing). */
    private boolean isZAxis() {
        if(level == null) return false;
        Direction facing = level.getBlockState(worldPosition).getValue(TransitionSealBlock.FACING);
        return facing == Direction.EAST || facing == Direction.WEST;
    }

    private boolean hasSignalAt(int dy, int dz, boolean zAxis) {
        BlockPos pos = zAxis ? worldPosition.offset(0, dy, dz) : worldPosition.offset(dz, dy, 0);
        return level.hasNeighborSignal(pos);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition, worldPosition.offset(0, 24, 0)).inflate(13);
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

    public void updateEntity() {
        if(level == null) return;

        boolean powered = isPowered();
        float prev = animTicks;
        boolean prevOpen = prev >= DURATION_TICKS;
        boolean wasMoving = moving;

        // Toggle on rising edge: a single redstone pulse will start the animation toward
        // the opposite state and it will continue until fully open/closed without
        // requiring the power to be held.
        if(powered && !prevPowered) {
            opening = !isOpen();
            moving = true;
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
                level.playSound(null, worldPosition, opening ? ModSounds.TRANSITION_SEAL_OPEN.get() : ModSounds.TRANSITION_SEAL_CLOSE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
            if(!moving && wasMoving) {
                level.playSound(null, worldPosition, ModSounds.METAL_STOP_1.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            }
            if(animTicks != prev) setChanged();
            if(isOpen() != prevOpen) {
                updatePartStates();
                setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("animTicks", animTicks);
        tag.putBoolean("prevPowered", prevPowered);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        animTicks = tag.getFloat("animTicks");
        prevPowered = tag.getBoolean("prevPowered");
    }
}
