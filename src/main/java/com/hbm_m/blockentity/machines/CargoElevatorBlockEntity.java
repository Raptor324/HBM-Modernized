package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.LoadedMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Порт {@code TileEntityCargoElevator} из 1.7.10.
 * <p>
 * Грузовой лифт: 3×3 база-мультиблок, платформа выдвигается вверх
 * на {@code height} блоков. Коллизия с сущностями — ручное перемещение
 * через {@code getEntitiesOfClass} + {@code moveTo} (не через entity-хитбокс).
 */
public class CargoElevatorBlockEntity extends LoadedMachineBlockEntity {

    public int height = 0;

    public double extension;
    public double prevExtension;
    public double syncExtension;
    private int sync;

    public boolean isExtending;
    /** 2 блока в секунду (оригинал: {@code speed = 2D / 20D}). */
    public static final double speed = 2D / 20D;
    public boolean renderPlatform = false;

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARGO_ELEVATOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CargoElevatorBlockEntity be) {
        be.prevExtension = be.extension;

        if (!level.isClientSide) {
            // Сервер: анимация выдвижения
            if (be.isExtending && be.extension < be.height) {
                be.extension += be.speed;
            }
            if (!be.isExtending && be.extension > 0) {
                be.extension -= be.speed;
            }
            be.extension = Mth.clamp(be.extension, 0, be.height);

            // Существуем хотя бы один тик перед рендером платформы —
            // фикс короткого мерцания из оригинала
            be.renderPlatform = true;

            be.setChanged();
            be.sendUpdateToClient();
        } else {
            // Клиент: интерполяция extension к syncExtension
            if (be.sync > 0) {
                be.extension = be.extension + ((be.syncExtension - be.extension) / (float) be.sync);
                --be.sync;
            } else {
                be.extension = be.syncExtension;
            }
        }

        // Обе стороны: перемещение сущностей на платформе
        if (be.extension != be.prevExtension) {
            double liftUpper = be.worldPosition.getY() + 1 + Math.max(be.extension, be.prevExtension);
            double liftLower = be.worldPosition.getY() + 1 + Math.min(be.extension, be.prevExtension);
            AABB box = new AABB(
                    be.worldPosition.getX() - 0.99, liftLower, be.worldPosition.getZ() - 0.99,
                    be.worldPosition.getX() + 1.99, liftUpper, be.worldPosition.getZ() + 1.99);

            List<Entity> toLift = level.getEntitiesOfClass(Entity.class, box);
            for (Entity e : toLift) {
                // Игроки перемещаются только на клиенте (как в оригинале)
                if (e instanceof net.minecraft.world.entity.player.Player && !level.isClientSide) continue;
                double entityBottom = e.getBoundingBox().minY;
                if (entityBottom >= liftLower && entityBottom <= liftUpper) {
                    double delta = entityBottom - (be.worldPosition.getY() + 1 + be.extension);
                    e.moveTo(e.getX(), e.getY() - delta, e.getZ());
                    e.setOnGround(true);
                    e.moveTo(e.getX(), e.getY() - 0.125, e.getZ());
                }
            }
        }
    }

    public void toggleElevator() {
        if (this.extension >= this.height) {
            this.isExtending = false;
        }
        if (this.extension <= 0) {
            this.isExtending = true;
        }
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("extension", extension);
        tag.putBoolean("isExtending", isExtending);
        tag.putInt("height", height);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putDouble("extension", extension);
        tag.putBoolean("isExtending", isExtending);
        tag.putInt("height", height);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.extension = tag.getDouble("extension");
        this.isExtending = tag.getBoolean("isExtending");
        this.height = tag.getInt("height");
        // Клиентская синхронизация: renderPlatform + syncExtension приходят через getUpdateTag
        this.renderPlatform = tag.getBoolean("renderPlatform");
        this.syncExtension = tag.getDouble("extension");
        if (this.syncExtension > 0 && this.syncExtension < this.height) {
            this.sync = 3;
        }
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        this.extension = tag.getDouble("extension");
        this.isExtending = tag.getBoolean("isExtending");
        this.height = tag.getInt("height");
        // Клиентская синхронизация: renderPlatform + syncExtension приходят через getUpdateTag
        this.renderPlatform = tag.getBoolean("renderPlatform");
        this.syncExtension = tag.getDouble("extension");
        if (this.syncExtension > 0 && this.syncExtension < this.height) {
            this.sync = 3;
        }
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("renderPlatform", renderPlatform);
        tag.putInt("height", height);
        tag.putDouble("extension", extension);
        return tag;
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {

        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("renderPlatform", renderPlatform);
        tag.putInt("height", height);
        tag.putDouble("extension", extension);
        return tag;
    
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    /**
     * Render AABB: динамический по высоте (оригинал: {@code getRenderBoundingBox}).
     */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        int h = 1 + this.height;
        return new AABB(
                worldPosition.getX() - 1,
                worldPosition.getY(),
                worldPosition.getZ() - 1,
                worldPosition.getX() + 2,
                worldPosition.getY() + h,
                worldPosition.getZ() + 2);
    }
}
