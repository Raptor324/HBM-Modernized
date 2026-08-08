package com.hbm_m.blockentity.machines;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.multiblock.IDummyCorePart;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Cargo elevator: a self-stacking 3x3 shaft (see {@link com.hbm_m.block.machines.CargoElevatorBlock})
 * with a platform that slides up/down the shaft. Only the core instance (bottom-center block)
 * carries real state; every other block of the shaft just points at the core via
 * {@link IDummyCorePart#getCorePos()}.
 */
public class CargoElevatorBlockEntity extends BlockEntity implements IDummyCorePart {

    public static final double SPEED = 2.0 / 20.0; // 2 blocks/second

    @Nullable
    private BlockPos corePos;

    /** Core-only: number of floors above the core (0 = single floor, no extra height). */
    public int height = 0;
    public double extension = 0;
    public double prevExtension = 0;
    public boolean isExtending = false;

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

    public static void tick(Level level, BlockPos pos, BlockState state, CargoElevatorBlockEntity be) {
        if (!be.isCore()) {
            return;
        }

        be.prevExtension = be.extension;

        if (be.isExtending && be.extension < be.height) {
            be.extension = Math.min(be.height, be.extension + SPEED);
        } else if (!be.isExtending && be.extension > 0) {
            be.extension = Math.max(0, be.extension - SPEED);
        }

        if (!level.isClientSide) {
            if (be.extension != be.prevExtension) {
                be.liftEntities(level, pos);
                be.setChanged();
                be.sendUpdateToClient();
            }
        }
    }

    private void liftEntities(Level level, BlockPos corePos) {
        double liftLower = corePos.getY() + 1 + Math.min(extension, prevExtension);
        double liftUpper = corePos.getY() + 1 + Math.max(extension, prevExtension) + 1;
        AABB scanBox = new AABB(
                corePos.getX() - 1, liftLower, corePos.getZ() - 1,
                corePos.getX() + 2, liftUpper, corePos.getZ() + 2);

        List<Entity> toLift = level.getEntitiesOfClass(Entity.class, scanBox);
        double platformY = corePos.getY() + 1 + extension;
        for (Entity e : toLift) {
            double feetY = e.getBoundingBox().minY;
            if (feetY >= liftLower - 0.5 && feetY <= liftUpper) {
                double delta = feetY - platformY;
                if (Math.abs(delta) < 1.0) {
                    e.setPos(e.getX(), platformY, e.getZ());
                    e.setOnGround(true);
                }
            }
        }
    }

    /** Toggles the platform between fully extended and fully retracted. Only meaningful on the core. */
    public void toggleElevator() {
        if (extension >= height) {
            isExtending = false;
        }
        if (extension <= 0) {
            isExtending = true;
        }
        setChanged();
        sendUpdateToClient();
    }

    /** Adds one floor to the shaft (called after a new 3x3 layer has been placed above the current top). */
    public void addFloor() {
        height++;
        setChanged();
        sendUpdateToClient();
    }

    /** Interpolated extension for rendering, in blocks above the core's own cell. */
    public double getRenderExtension(float partialTick) {
        return Mth.lerp(partialTick, prevExtension, extension);
    }

    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide && !isRemoved()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (corePos != null) {
            tag.put("CorePos", NbtUtils.writeBlockPos(corePos));
        }
        tag.putInt("height", height);
        tag.putDouble("extension", extension);
        tag.putBoolean("isExtending", isExtending);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        corePos = tag.contains("CorePos") ? NbtUtils.readBlockPos(tag.getCompound("CorePos")) : null;
        height = tag.getInt("height");
        extension = tag.getDouble("extension");
        prevExtension = extension;
        isExtending = tag.getBoolean("isExtending");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    //? if forge {
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
    //?}

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if forge {
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) {
            load(pkt.getTag());
        }
    }
    //?}
}
