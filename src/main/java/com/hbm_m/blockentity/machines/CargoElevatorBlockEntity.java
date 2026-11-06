package com.hbm_m.blockentity.machines;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.block.machines.CargoElevatorBlock;
import com.hbm_m.multiblock.IDummyCorePart;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cargo elevator: a self-stacking 3x3 shaft (see {@link CargoElevatorBlock})
 * with a platform that slides up/down the shaft. Only the core instance (bottom-center block)
 * carries real state; every other block of the shaft just points at the core via
 * {@link IDummyCorePart#getCorePos()}.
 *
 * <p>1:1 {@code TileEntityCargoElevator}: the motion is driven by {@link #targetExtension}
 * (0 = retracted, {@link #height} = fully extended), so a click reverses the platform mid-travel.
 */
public class CargoElevatorBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity
        implements IDummyCorePart, IRORInteractive {

    public static final double SPEED = 2.0 / 20.0; // 2 blocks per second

    @Nullable
    private BlockPos corePos;

    /** Core-only: number of floors above the core (0 = single floor, no extra height). */
    public int height = 0;
    public int targetExtension = 0;
    public double extension = 0;
    public double prevExtension = 0;
    /** Client-side interpolation target, 1:1 {@code syncExtension}. */
    public double syncExtension = 0;
    /** True once the server has ticked at least once, gates first-frame platform render (1:1 {@code renderPlatform}). */
    public boolean renderPlatform = false;
    public boolean dropped = false;

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_ELEVATOR_BE.get(), pos, state);
    }

    @Nullable
    @Override
    public BlockPos getRawCorePos() {
        return corePos;
    }

    @Override
    public void setRawCorePos(@Nullable BlockPos pos) {
        this.corePos = pos;
        setChanged();
    }

    /** Cells check their core on a slow cadence; the core itself has nothing to verify. */
    private static final int PART_CHECK_INTERVAL = 40;

    public static void tick(Level level, BlockPos pos, BlockState state, CargoElevatorBlockEntity be) {
        if (!be.isCore()) {
            if (!level.isClientSide
                    && Math.floorMod(level.getGameTime() + pos.hashCode(), PART_CHECK_INTERVAL) == 0) {
                be.verifyCore(level, pos);
            }
            return;
        }

        be.prevExtension = be.extension;

        if (!level.isClientSide) {

            // connect to lower elevator (1:1 TileEntityCargoElevator.updateEntity)
            if (be.mergeIntoLowerElevator(level, pos)) {
                return;
            }

            if (be.extension < be.targetExtension) { // go up
                be.extension += SPEED;
                be.extension = Mth.clamp(be.extension, 0, be.targetExtension);
            } else if (be.extension > be.targetExtension) { // go down
                be.extension -= SPEED;
                be.extension = Mth.clamp(be.extension, be.targetExtension, be.height);
            }

            be.extension = Mth.clamp(be.extension, 0, be.height);

            // exist for at least one tick before the main portion gets rendered, fixes the short flickering platform that instantly despawns
            be.renderPlatform = true;
        } else {
            // Smooth client mirror: instead of easing toward the last received position over 3 ticks
            // (stair-stepping against the per-tick server packets), the client runs the same motion
            // toward the synced targetExtension and lets the packets only correct drift.
            if (be.extension < be.targetExtension) {
                be.extension = Math.min(be.targetExtension, be.extension + SPEED);
            } else if (be.extension > be.targetExtension) {
                be.extension = Math.max(be.targetExtension, be.extension - SPEED);
            }
            // gentle drift correction toward the authoritative server position, hard snap when far off
            double diff = be.syncExtension - be.extension;
            if (Math.abs(diff) > 1.0) {
                be.extension = be.syncExtension;
            } else {
                be.extension += diff * 0.1;
            }
        }

        if (be.extension != be.prevExtension) {
            be.liftEntities(level, pos);
            if (!level.isClientSide) {
                be.setChanged();
            }
        }

        if (!level.isClientSide) {
            // 1:1 networkPackNT(300)
            be.syncIfChangedOrEvery(be.stateFingerprint(), 0, 6);
        }
    }

    private long stateFingerprint() {
        return Double.doubleToLongBits(extension)
                ^ ((long) height << 32)
                ^ ((targetExtension & 0xFFFFFFFFL) << 1)
                ^ (renderPlatform ? 1L : 0L);
    }

    /**
     * 1:1 of the {@code // connect to lower elevator} block in {@code TileEntityCargoElevator.updateEntity}:
     * a freshly assembled stack standing directly on top of another one is absorbed by the lower core.
     */
    private boolean mergeIntoLowerElevator(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!(level.getBlockState(below).getBlock() instanceof CargoElevatorBlock)) {
            return false;
        }
        BlockPos lowerCore = CargoElevatorBlock.findCoreByGeometry(level, below);
        if (lowerCore == null || lowerCore.getX() != pos.getX() || lowerCore.getZ() != pos.getZ()
                || lowerCore.equals(pos)) {
            return false;
        }
        if (!(level.getBlockEntity(lowerCore) instanceof CargoElevatorBlockEntity lower)) {
            return false;
        }

        lower.height += this.height + 1;
        lower.setChanged();
        lower.sendUpdateToClient();

        for (int dy = 0; dy <= this.height; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    level.setBlock(p, com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(p) instanceof com.hbm_m.interfaces.IMultiblockPart part) {
                        part.setControllerPos(lowerCore);
                        part.setLocalOffsetFromController(new BlockPos(dx, pos.getY() + dy - lowerCore.getY(), dz));
                    }
                }
            }
        }
        return true;
    }

    /**
     * Keeps a cell attached to its core, the way BlockDummyable.destroyIfOrphan does upstream.
     *
     * <p>An assembly engine moves the cells without touching the pointer they store, so a shaft
     * that travelled on a ship names pre-move positions; the geometry of the shaft itself is the
     * only thing that survived the trip, so the core is re-derived from it. A cell that has no
     * core at all is litter and removes itself - but only once the position it names is loaded,
     * otherwise an unlucky chunk boundary would eat a perfectly good shaft.
     */
    private void verifyCore(Level level, BlockPos pos) {
        if (com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        if (resolveCore(CargoElevatorBlockEntity.class) != null) return;

        BlockPos found = CargoElevatorBlock.findCoreByGeometry(level, pos);
        if (found != null) {
            setCorePos(found);
            setChanged();
            return;
        }
        if (level.hasChunkAt(getCorePos())) {
            level.removeBlock(pos, false);
        }
    }

    /** 1:1 TileEntityCargoElevator.updateEntity entity sweep. */
    private void liftEntities(Level level, BlockPos corePos) {
        double liftUpper = corePos.getY() + 1 + Math.max(this.extension, this.prevExtension);
        double liftLower = corePos.getY() + 1 + Math.min(this.extension, this.prevExtension);
        AABB scanBox = new AABB(
                corePos.getX() - 0.99, liftLower, corePos.getZ() - 0.99,
                corePos.getX() + 1.99, liftUpper, corePos.getZ() + 1.99);

        List<Entity> toLift = level.getEntitiesOfClass(Entity.class, scanBox);

        for (Entity e : toLift) {
            if (e instanceof Player && !level.isClientSide) continue;
            if (e.getBoundingBox().minY >= liftLower && e.getBoundingBox().minY <= liftUpper) {
                double delta = e.getBoundingBox().minY - (corePos.getY() + 1 + this.extension);
                e.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -delta, 0));
                e.setOnGround(true);
                e.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(0, -0.125, 0));
            }
        }
    }

    /** 1:1 TileEntityCargoElevator.toggleElevator: a click reverses the platform mid-travel. */
    public void toggleElevator() {
        if (targetExtension == 0) {
            targetExtension = this.height;
        } else {
            targetExtension = 0;
        }
        setChanged();
    }

    /** Adds one floor to the shaft (called after a new 3x3 layer has been placed above the current top). */
    public void addFloor() {
        height++;
        setChanged();
        sendUpdateToClient();
    }

    /** Removes the top floor from the shaft (screwdriver dismantle), keeping the platform reachable. */
    public void removeFloor() {
        if (height > 0) {
            height--;
        }
        if (targetExtension > height) {
            targetExtension = height;
        }
        setChanged();
        sendUpdateToClient();
    }

    /** Interpolated extension for rendering, in blocks above the core's own cell. */
    public double getRenderExtension(float partialTick) {
        return Mth.lerp(partialTick, prevExtension, extension);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (corePos != null) {
            com.hbm_m.platform.BlockHooks.writeBlockPos(tag, "CorePos", corePos);
        }
        tag.putInt("height", height);
        tag.putInt("targetExtension", targetExtension);
        tag.putDouble("extension", extension);
        tag.putBoolean("renderPlatform", renderPlatform);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        corePos = tag.contains("CorePos") ? com.hbm_m.platform.BlockHooks.readBlockPos(tag, "CorePos") : null;
        height = tag.getInt("height");
        targetExtension = tag.getInt("targetExtension");
        extension = tag.getDouble("extension");
        prevExtension = extension;
        renderPlatform = tag.getBoolean("renderPlatform");
    }

    /** Client update: take the authoritative position as the drift-correction target, keep the local mirror running. */
    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        applyClientUpdate(tag, null);
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        double current = this.extension;
        readNbtData(tag, registries);
        this.syncExtension = this.extension;
        this.extension = current;
        this.prevExtension = current;
    }

    /** 1:1 TileEntityCargoElevator.getRenderBoundingBox (Render-Migration: Raptors Pfad cullt nach dieser Box, der alte BER zeichnete immer). */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        double top = Math.max(height + 1.0, extension + 1.25) + 1.0;
        return new net.minecraft.world.phys.AABB(
            worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
            worldPosition.getX() + 2, worldPosition.getY() + top, worldPosition.getZ() + 2
        );
    }

    // ═════════════════════════════════════════════════════════════════════════════════
    //  1:1 IRORInteractive
    // ═════════════════════════════════════════════════════════════════════════════════

    @Override
    @Nullable
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setextension").equals(name) && params.length > 0) {
            targetExtension = IRORInteractive.parseInt(params[0], 0, height);
            return null;
        }
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
            PREFIX_VALUE + "extension",
            PREFIX_FUNCTION + "setextension" + NAME_SEPARATOR + "amount",
        };
    }
}
